package com.sam_chordas.android.stockhawk.ui;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.AsyncTask;
import android.os.Bundle;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, 0);

        if (getArguments() != null) {
            GetStockHistoryTask historyLoader = new GetStockHistoryTask();
            historyLoader.execute(getArguments().getString("stock"));
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

        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    private class GetStockHistoryTask extends AsyncTask<String, Void, LineSet> {
        @Override
        protected LineSet doInBackground(String... params) {
            if (params.length == 0)
                return null;

            Calendar cal = Calendar.getInstance();
            Date endDate = cal.getTime();
            cal.add(Calendar.MONTH, -6);
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

                    return set;

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(LineSet set) {
            if (set != null) {
                int min = (int) Math.floor(set.getMin().getValue());
                int max = (int) Math.ceil(set.getMax().getValue());
                int step = (max-min)/10;
                mChart.setAxisBorderValues(min-5,
                        max);
                mChart.setStep((step > 1) ? step : 1);
                mChart.addData(set);
                mChart.show();
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
}
