package catalogue_generator;

import java.util.ArrayList;
import java.util.ListIterator;

import org.eclipse.swt.widgets.Listener;

/**
 * Manager used to manage several threads. It is possible
 * to start the threads in batches, limiting the maximum
 * number of threads active at runtime (save computational power
 * and memory)
 * @author avonva
 *
 */
public class CatalogueDownloaderManager extends Thread {

	private ArrayList<CatalogueDownloader> threads;
	private int batchSize;
	private Listener doneListener;
	private int startedCount = 0;

	/**
	 * Initialize the manager
	 * @param batchSize maximum number of threads which can
	 * be active at runtime in the same time
	 */
	public CatalogueDownloaderManager( int batchSize ) {
		this.threads = new ArrayList<>();
		this.batchSize = batchSize;
	}

	/**
	 * Called when all the threads finishes
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}

	/**
	 * Plan a new thread
	 * @param downloader
	 */
	public void add( CatalogueDownloader downloader ) {
		threads.add( downloader );
	}

	@Override
	public void run() {

		if ( batchSize > threads.size() ) {
			System.err.println( "CatalogueDownloaderManager: Cannot create a batch size greater "
					+ "that the available threads. Setting to " + threads.size() );
			batchSize = threads.size();
		}

		ListIterator<CatalogueDownloader> list = threads.listIterator();

		// continue until we have something
		while ( list.hasNext() ) {
			
			// proceed if we can start a thread
			if ( startedCount < batchSize ) {

				// start the next thread
				CatalogueDownloader thread = list.next();

				// when finished decrease the thread count
				thread.setDoneListener( new ThreadFinishedListener() {

					@Override
					public void finished(Thread thread, int code) {
						startedCount--;
					}
				});

				thread.start();

				startedCount++;
			}
			else {  // Otherwise wait for a while
				sleepFor(300);
			}
		} // end while
		
		// wait remaining threads to finish
		for ( CatalogueDownloader thread : threads ) {
			while ( !thread.isFinished() )
				sleepFor(300);
		}

		if ( doneListener != null )
			doneListener.handleEvent( null );
	}
	
	private void sleepFor ( long time ) {
		try {
			Thread.sleep( time );
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
