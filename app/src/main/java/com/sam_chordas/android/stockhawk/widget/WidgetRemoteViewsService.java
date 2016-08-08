package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.database.Cursor;
import android.os.Binder;
import android.widget.AdapterView;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;


/**
 * Created by Harry Grillo on 8/2/16.
 */

public class WidgetRemoteViewsService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new RemoteViewsFactory() {
            private Cursor data = null;

            @Override
            public void onCreate() {

            }

            @Override
            public void onDataSetChanged() {
                if (data != null)
                    data.close();

                final long identityToken = Binder.clearCallingIdentity();
                data = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{QuoteColumns._ID, QuoteColumns.NAME, QuoteColumns.SYMBOL,
                        QuoteColumns.BIDPRICE, QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE,
                        QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " =?",
                        new String[]{"1"},
                        null);
                Binder.restoreCallingIdentity(identityToken);
            }

            @Override
            public void onDestroy() {
                if (data != null) {
                    data.close();
                    data = null;
                }
            }

            @Override
            public int getCount() {
                return data == null ? 0 : data.getCount();
            }

            @Override
            public RemoteViews getViewAt(int position) {
                if (position == AdapterView.INVALID_POSITION
                        || data == null || !data.moveToPosition(position))
                    return null;

                RemoteViews views =
                        new RemoteViews(getPackageName(), R.layout.widget_stock_list_item);

                String companyName = data.getString(data.getColumnIndex("name"));
                String symbol = data.getString(data.getColumnIndex("symbol"));
                views.setTextViewText(R.id.stock_symbol, symbol);
                String bidPrice = data.getString(data.getColumnIndex("bid_price"));
                views.setTextViewText(R.id.bid_price, bidPrice);

                final int backgroundDrawableValue = data.getInt(data.getColumnIndex("is_up")) == 1 ?
                        R.drawable.percent_change_pill_green : R.drawable.percent_change_pill_red;
                views.setInt(R.id.change, "setBackgroundResource", backgroundDrawableValue);

                String change = data.getString(data.getColumnIndex(
                        Utils.showPercent ? "percent_change" : "change"));
                views.setTextViewText(R.id.change, change);

                views.setContentDescription(R.id.widget_list_item,
                        String.format(getString(R.string.item_view_description),
                        companyName, bidPrice, change));

                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(MyStocksActivity.STOCK_HISTORY_EXTRA_KEY, symbol);
                views.setOnClickFillInIntent(R.id.widget_list_item, fillInIntent);

                return views;
            }

            @Override
            public RemoteViews getLoadingView() {
                return new RemoteViews(getPackageName(), R.id.widget_list_item);
            }

            @Override
            public int getViewTypeCount() {
                return 1;
            }

            @Override
            public long getItemId(int position) {
                if (data.moveToPosition(position))
                    return data.getLong(data.getColumnIndex(QuoteColumns._ID));
                return position;
            }

            @Override
            public boolean hasStableIds() {
                return true;
            }
        };
    }
}
