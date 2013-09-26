package com.kuxhausen.huemore.editmood;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.kuxhausen.huemore.R;
import com.kuxhausen.huemore.state.api.BulbState;

public class StateCell {

	public String name;
	public BulbState hs;
	
	
	public View getView(int position, ViewGroup parent, OnClickListener l, SherlockFragment frag) {
		View rowView;
		LayoutInflater inflater = frag.getActivity().getLayoutInflater();
		if(hs.ct!=null && hs.ct!=0){
			rowView = inflater.inflate(R.layout.edit_mood_colortemp_row, null);
			TextView stateText = (TextView) rowView.findViewById(R.id.ctTextView);
			stateText.setText(hs.getCT());
		}else{
			rowView = inflater.inflate(R.layout.edit_mood_row, null);

			ImageView state_color = (ImageView) rowView
					.findViewById(R.id.stateColorView);
			int color = 0;
			if(hs.hue!=null && hs.sat!=null){
				float[] hsv = new float[3];
		    	hsv[0] = (float) ((hs.hue *360)/ 65535.0) ;
		    	hsv[1] = (float) (hs.sat / 255.0);
		    	hsv[2] = 1f;
		    	color = Color.HSVToColor(hsv);
			}
			ColorDrawable cd = new ColorDrawable(color);
			cd.setAlpha(255);
			if((color%0xff000000)!=0)
				state_color.setImageDrawable(cd);		
		}
		rowView.setOnClickListener(l);
		if(frag!=null)
			frag.registerForContextMenu(rowView);
		rowView.setTag(position);
		return rowView;
	}
}