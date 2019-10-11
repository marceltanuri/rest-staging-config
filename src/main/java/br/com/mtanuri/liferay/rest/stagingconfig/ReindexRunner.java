package br.com.mtanuri.liferay.rest.stagingconfig;

import com.liferay.portal.kernel.cluster.Address;
import com.liferay.portal.kernel.cluster.ClusterExecutorUtil;
import com.liferay.portal.kernel.cluster.ClusterRequest;
import com.liferay.portal.kernel.concurrent.ThreadPoolExecutor;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.executor.PortalExecutorManagerUtil;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.messaging.BaseAsyncDestination;
import com.liferay.portal.kernel.messaging.Destination;
import com.liferay.portal.kernel.messaging.MessageBus;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.search.SearchEngineUtil;
import com.liferay.portal.kernel.util.ClassResolverUtil;
import com.liferay.portal.kernel.util.MethodHandler;
import com.liferay.portal.kernel.util.MethodKey;
import com.liferay.portal.kernel.util.StringBundler;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portlet.admin.action.EditServerAction;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ReindexRunner implements Runnable {

	private static final long COMPANYID = 10155L;
	private static Log _log = LogFactoryUtil.getLog(ReindexRunner.class);

	public ReindexRunner() {

	}

	public void run() {

		try {

			Class<?> resolveByPortalClassLoader = ClassResolverUtil
					.resolveByPortalClassLoader("com.liferay.portal.search.lucene.LuceneIndexer");
			Constructor<?> cons = resolveByPortalClassLoader.getConstructor(Long.class);
			Object o = cons.newInstance(COMPANYID);

			Method reindex = resolveByPortalClassLoader.getMethod("reindex");
			Method getUsedSearchEngineIds = resolveByPortalClassLoader.getMethod("getUsedSearchEngineIds");

			Set<String> usedSearchEngineIds = new HashSet<String>();

			try {
				reindex.invoke(o);
				@SuppressWarnings("unchecked")
				Collection<String> invoke = (Collection<String>) getUsedSearchEngineIds.invoke(o);
				usedSearchEngineIds.addAll(invoke);

			} catch (Exception e) {
				_log.error(e, e);
			}

			Set<BaseAsyncDestination> searchWriterDestinations = new HashSet<BaseAsyncDestination>();

			MessageBus messageBus = MessageBusUtil.getMessageBus();

			for (String usedSearchEngineId : usedSearchEngineIds) {
				String searchWriterDestinationName = SearchEngineUtil
						.getSearchWriterDestinationName(usedSearchEngineId);

				Destination destination = messageBus.getDestination(searchWriterDestinationName);

				if (destination instanceof BaseAsyncDestination) {
					BaseAsyncDestination baseAsyncDestination = (BaseAsyncDestination) destination;

					searchWriterDestinations.add(baseAsyncDestination);
				}
			}

			try {
				long[] companyIds = { COMPANYID };
				submitClusterIndexLoadingSyncJob(searchWriterDestinations, companyIds);
			} catch (Exception e) {
				_log.error(e, e);
			}
		} catch (Exception e) {
			_log.error(e, e);
		}
	}

	protected void submitClusterIndexLoadingSyncJob(Set<BaseAsyncDestination> baseAsyncDestinations, long[] companyIds)
			throws Exception {

		if (_log.isInfoEnabled()) {
			StringBundler sb = new StringBundler(baseAsyncDestinations.size() + 1);

			sb.append("[");

			for (BaseAsyncDestination baseAsyncDestination : baseAsyncDestinations) {

				sb.append(baseAsyncDestination.getName());
				sb.append(", ");
			}

			sb.setStringAt("]", sb.index() - 1);

			_log.info("Synchronizecluster index loading for destinations " + sb.toString());
		}

		int totalWorkersMaxSize = 0;

		for (BaseAsyncDestination baseAsyncDestination : baseAsyncDestinations) {

			totalWorkersMaxSize += baseAsyncDestination.getWorkersMaxSize();
		}

		if (_log.isInfoEnabled()) {
			_log.info("There are " + totalWorkersMaxSize + " synchronization threads");
		}

		CountDownLatch countDownLatch = new CountDownLatch(totalWorkersMaxSize + 1);

		ClusterLoadingSyncJob slaveClusterLoadingSyncJob = new ClusterLoadingSyncJob(companyIds, countDownLatch, false);

		for (BaseAsyncDestination baseAsyncDestination : baseAsyncDestinations) {

			ThreadPoolExecutor threadPoolExecutor = PortalExecutorManagerUtil
					.getPortalExecutor(baseAsyncDestination.getName());

			for (int i = 0; i < baseAsyncDestination.getWorkersMaxSize(); i++) {
				threadPoolExecutor.execute(slaveClusterLoadingSyncJob);
			}
		}

		ClusterLoadingSyncJob masterClusterLoadingSyncJob = new ClusterLoadingSyncJob(companyIds, countDownLatch, true);

		ThreadPoolExecutor threadPoolExecutor = PortalExecutorManagerUtil
				.getPortalExecutor(EditServerAction.class.getName());

		threadPoolExecutor.execute(masterClusterLoadingSyncJob);
	}

	private static class ClusterLoadingSyncJob implements Runnable {

		public ClusterLoadingSyncJob(long[] companyIds, CountDownLatch countDownLatch, boolean master) {

			_companyIds = companyIds;
			_countDownLatch = countDownLatch;
			_master = master;
		}

		@Override
		public void run() {
			_countDownLatch.countDown();

			String logPrefix = StringPool.BLANK;

			if (_log.isInfoEnabled()) {
				Thread currentThread = Thread.currentThread();

				if (_master) {
					logPrefix = "Monitor thread name " + currentThread.getName() + " with thread ID "
							+ currentThread.getId();
				} else {
					logPrefix = "Thread name " + currentThread.getName() + " with thread ID " + currentThread.getId();
				}
			}

			if (!_master && _log.isInfoEnabled()) {
				_log.info(logPrefix + " synchronized on latch. Waiting for others.");
			}

			try {
				if (_master) {
					_countDownLatch.await();
				} else {
					boolean result = _countDownLatch.await(60000, TimeUnit.MILLISECONDS);

					if (!result) {
						_log.error(logPrefix + " timed out. You may need to " + "re-trigger a reindex process.");
					}
				}
			} catch (InterruptedException ie) {
				if (_master) {
					_log.error(logPrefix + " was interrupted. Skip cluster index " + "loading notification.", ie);

					return;
				} else {
					_log.error(logPrefix + " was interrupted. You may need to " + "re-trigger a reindex process.", ie);
				}
			}

			if (_master) {
				Address localClusterNodeAddress = ClusterExecutorUtil.getLocalClusterNodeAddress();

				ClusterRequest clusterRequest = ClusterRequest.createMulticastRequest(
						new MethodHandler(_loadIndexesFromClusterMethodKey, _companyIds, localClusterNodeAddress),
						true);

				try {
					ClusterExecutorUtil.execute(clusterRequest);
				} catch (SystemException se) {
					_log.error("Unable to notify peers to start index loading", se);
				}

				if (_log.isInfoEnabled()) {
					_log.info(logPrefix + " unlocked latch. Notified peers to " + "start index loading.");
				}
			}
		}

		private long[] _companyIds;
		private CountDownLatch _countDownLatch;
		private boolean _master;

	}

	private static MethodKey _loadIndexesFromClusterMethodKey = new MethodKey(
			ClassResolverUtil.resolveByPortalClassLoader("com.liferay.portal.search.lucene.cluster.LuceneClusterUtil"),
			"loadIndexesFromCluster", long[].class, Address.class);

}
