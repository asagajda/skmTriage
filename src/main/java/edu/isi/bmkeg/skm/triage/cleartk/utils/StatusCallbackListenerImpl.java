package edu.isi.bmkeg.skm.triage.cleartk.utils;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.collection.EntityProcessStatus;
import org.apache.uima.collection.StatusCallbackListener;

/**
 * Callback Listener. Receives event notifications from CPE.
 * 
 * 
 */
class StatusCallbackListenerImpl implements StatusCallbackListener {

	private int entityCount = 0;
	private long size = 0;
	private long mStartTime;

	public int getEntityCount() {
		return entityCount;
	}

	public void setEntityCount(int entityCount) {
		this.entityCount = entityCount;
	}
	
	
	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public long getMstartTime() {
		return mStartTime;
	}

	public void setMstartTime(long mstartTime) {
		this.mStartTime = mstartTime;
	}

	/**
	 * Called when the initialization is completed.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#initializationComplete()
	 */
	public void initializationComplete() {
		System.out.println("CPM Initialization Complete");
	}

	/**
	 * Called when the batchProcessing is completed.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#batchProcessComplete()
	 * 
	 */
	public void batchProcessComplete() {
		System.out.println("Completed " + entityCount + " documents; " + size
				+ " characters");
		long elapsedTime = System.currentTimeMillis() - mStartTime;
		System.out.println("Time Elapsed : " + elapsedTime + " ms ");
	}

	/**
	 * Called when the collection processing is completed.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#collectionProcessComplete()
	 */
	public void collectionProcessComplete() {
		System.out.println("Completed " + entityCount + " documents; " + size
				+ " characters");
		long elapsedTime = System.currentTimeMillis() - mStartTime;
		System.out.println("Time Elapsed : " + elapsedTime + " ms ");
		System.out
				.println("\n\n ------------------ PERFORMANCE REPORT ------------------\n");
		// System.out.println(mCPE.));

	}

	/**
	 * Called when the CPM is paused.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#paused()
	 */
	public void paused() {
		System.out.println("Paused");
	}

	/**
	 * Called when the CPM is resumed after a pause.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#resumed()
	 */
	public void resumed() {
		System.out.println("Resumed");
	}

	/**
	 * Called when the CPM is stopped abruptly due to errors.
	 * 
	 * @see com.ibm.uima.collection.processing.StatusCallbackListener#aborted()
	 */
	public void aborted() {
		System.out.println("Aborted");
	}

	/**
	 * Called when the processing of a Document is completed. <br>
	 * The process status can be looked at and corresponding actions taken.
	 * 
	 * @param aCas
	 *            CAS corresponding to the completed processing
	 * @param aStatus
	 *            EntityProcessStatus that holds the status of all the events
	 *            for aEntity
	 */
	public void entityProcessComplete(CAS aCas, EntityProcessStatus aStatus) {

		if (aStatus.isException()) {
			List exceptions = aStatus.getExceptions();
			for (int i = 0; i < exceptions.size(); i++) {
				((Throwable) exceptions.get(i)).printStackTrace();
			}
			return;
		}
		entityCount++;
		String docText = aCas.getDocumentText();
		if (docText != null) {
			size += docText.length();
		}
	}


}