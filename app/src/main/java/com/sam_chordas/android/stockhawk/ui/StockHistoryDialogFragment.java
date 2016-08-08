package com.sam_chordas.android.stockhawk.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.db.chart.model.LineSet;
import com.db.chart.view.AxisController;
import com.db.chart.view.ChartView;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 * Created by Harry Grillo on 7/11/16.
 */

public class StockHistoryDialogFragment extends DialogFragment {
    private ChartView mChart;
    private Dialog mDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);

        Bundle args = getArguments();
        if (args != null) {
            GetStockHistoryTask historyLoader = new GetStockHistoryTask();
            historyLoader.execute(args.getString("stock"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_line_graph, container, false);
        mChart = (LineChartView) v.findViewById(R.id.linechart);
        mChart.setXLabels(AxisController.LabelPosition.NONE);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        mDialog = getDialog();
        if (mDialog != null) {
            mDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private class GetStockHistoryTask extends AsyncTask<String, Void, LineSetWrapper> {
        @Override
        protected LineSetWrapper doInBackground(String... params) {
            if (params.length == 0)
                return null;

            Calendar cal = Calendar.getInstance();
            Date endDate = cal.getTime();
            cal.add(Calendar.MONTH, -3);
            Date startDate = cal.getTime();

            String urlString = YahooStockApi.getStockHistoryUrl(params[0], startDate, endDate);
            String getResponse;

            try {
                Request request = new Request.Builder().url(urlString).build();
                getResponse = new OkHttpClient().newCall(request).execute().body().string();

                try {
                    JSONArray dataPoints = new JSONObject(getResponse).getJSONObject("query")
                            .getJSONObject("results")
                            .getJSONArray("quote");

                    LineSet set = new LineSet();

                    JSONObject point;
                    for (int i = 0; i < dataPoints.length(); i++) {
                        point = dataPoints.getJSONObject(i);
                        set.addPoint(point.getString("Date"), (float) point.getDouble("Close"));
                    }

                    return new LineSetWrapper(dataPoints.getJSONObject(0).getString("Symbol"), set);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(LineSetWrapper wrapper) {
            LineSet set = wrapper.set;
            if (set != null) {
                float diff = set.getValue(0) - set.getValue(1);
                int color = ContextCompat.getColor(getActivity(), (diff < 0)
                        ? R.color.material_red_700 : R.color.material_green_700);
                mChart.setAxisColor(color);
                mChart.setLabelsColor(color);

                int min = (int) Math.floor(set.getMin().getValue());
                int max = (int) Math.ceil(set.getMax().getValue());
                int step = (max-min)/10;
                set.setColor(color);
                mChart.setAxisBorderValues(min - new Double((max-min)*0.3).intValue(),
                        max);
                mChart.setStep((step > 1) ? step : 1);
                mChart.addData(set);
                mChart.show();

                mDialog.setTitle(wrapper.symbol);
            }
        }
    }

    private static class YahooStockApi {
        static String getStockHistoryUrl(String symbol, Date startDate, Date endDate) {
            DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
            StringBuilder urlStringBuilder = new StringBuilder();
            try {
                urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
                urlStringBuilder.append(URLEncoder.encode(
                        "select * from yahoo.finance.historicaldata where symbol = \"" + symbol +
                                "\" and startDate = \"" + format.format(startDate) +
                                "\" and endDate = \"" + format.format(endDate) + "\"", "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                    + "org%2Falltableswithkeys&callback=");

            return urlStringBuilder.toString();
        }
    }


    protected class LineSetWrapper {
        public String symbol;
        public LineSet set;

        public LineSetWrapper(String symbol, LineSet set) {
            this.symbol = symbol;
            this.set = set;
        }
    }
}
