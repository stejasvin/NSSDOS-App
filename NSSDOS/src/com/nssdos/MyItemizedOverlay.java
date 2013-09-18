package com.nssdos;

import java.util.List;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.OverlayItem;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.view.View.OnClickListener;

/** Simple implementation of an ItemizedOverlay handling tap events with osmdroid. 
 * 
 * Usage inside your map activity: <br>
 * <code>
 * ArrayList<OverlayItem> list = new ArrayList<OverlayItem>();
 * TappableItemizedOverlay<OverlayItem> overlay = new TappableItemizedOverlay(this, list);
 * mapView.getOverlays.add(overlay);
 * </code>
 * 
 * @author M.Kergall
 */

class MyItemizedOverlay extends ItemizedIconOverlay<OverlayItem> {
	protected Context mContext;
	
	public MyItemizedOverlay(final Context context, final List<OverlayItem> aList) {
		 super(context, aList, new OnItemGestureListener<OverlayItem>() {
            @Override public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                    return false;
            }
            @Override public boolean onItemLongPress(final int index, final OverlayItem item) {
                    return false;
            }
    	} );
		mContext = context;
	}
	
	@Override protected boolean onSingleTapUpHelper(final int index, final OverlayItem item, final MapView mapView) {
		//Toast.makeText(mContext, "Item " + index + " has been tapped!", Toast.LENGTH_SHORT).show();
		AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
		dialog.setTitle(item.getTitle());
		dialog.setMessage(item.getSnippet());
		dialog.setNegativeButton("Remove from map", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				MapActivity.searchAndRemoveAllImports(item.getSnippet());
				
			}
		});
		dialog.show();
		return true;
	}
	

}
