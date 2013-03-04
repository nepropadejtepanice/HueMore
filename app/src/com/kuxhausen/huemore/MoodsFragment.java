package com.kuxhausen.huemore;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.kuxhausen.huemore.DatabaseDefinitions.MoodColumns;

public class MoodsFragment extends ListFragment {
	final static String ARG_POSITION = "position";
	int mCurrentPosition = -1;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		// If activity recreated (such as from screen rotate), restore
		// the previous article selection set by onSaveInstanceState().
		// This is primarily necessary when in the two-pane layout.
		if (savedInstanceState != null) {
			mCurrentPosition = savedInstanceState.getInt(ARG_POSITION);
		}

		// We need to use a different list item layout for devices older than
		// Honeycomb
		int layout = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? android.R.layout.simple_list_item_activated_1
				: android.R.layout.simple_list_item_1;

		String[] columns = { MoodColumns.MOOD, MoodColumns._ID };
		
		CursorAdapter dataSource = new SimpleCursorAdapter(this.getActivity(),
				R.layout.mood_row,
				((MainActivity) this.getActivity()).helper.getMoodCursor(),
				columns, new int[] { R.id.moodTextView }, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);

		
		
		setListAdapter(dataSource);

		// Inflate the layout for this fragment
		return inflater.inflate(R.layout.mood_view, container, false);
	}

	@Override
	public void onStart() {
		super.onStart();

		// When in two-pane layout, set the listview to highlight the selected
		// list item
		// (We do this during onStart because at the point the listview is
		// available.)
		if (getFragmentManager().findFragmentById(R.id.groups_fragment) != null) {
			getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
		}

		// During startup, check if there are arguments passed to the fragment.
		// onStart is a good place to do this because the layout has already
		// been
		// applied to the fragment at this point so we can safely call the
		// method
		// below that sets the article text.
		Bundle args = getArguments();
		if (args != null) {
			// Set article based on argument passed in
			updateArticleView(args.getInt(ARG_POSITION));
		} else if (mCurrentPosition != -1) {
			// Set article based on saved instance state defined during
			// onCreateView
			updateArticleView(mCurrentPosition);
		}
	}

	public void updateArticleView(int position) {
		// TextView article = (TextView)
		// getActivity().findViewById(R.id.article);
		// article.setText(StaticDataStore.Moods[position]);
		mCurrentPosition = position;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		// Save the current article selection in case we need to recreate the
		// fragment
		outState.putInt(ARG_POSITION, mCurrentPosition);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {

		// Set the item as checked to be highlighted when in two-pane layout
		getListView().setItemChecked(position, true);
	}
}
