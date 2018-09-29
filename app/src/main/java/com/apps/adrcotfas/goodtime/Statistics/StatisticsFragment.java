package com.apps.adrcotfas.goodtime.Statistics;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.adrcotfas.goodtime.Database.AppDatabase;
import com.apps.adrcotfas.goodtime.LabelAndColor;
import com.apps.adrcotfas.goodtime.R;
import com.apps.adrcotfas.goodtime.Session;
import com.apps.adrcotfas.goodtime.databinding.StatisticsMainBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.joda.time.LocalDate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

import static android.app.Activity.RESULT_OK;
import static com.apps.adrcotfas.goodtime.Statistics.SpinnerStatsType.DURATION;
import static com.apps.adrcotfas.goodtime.Util.StringUtils.formatDateAndTime;
import static com.apps.adrcotfas.goodtime.Util.StringUtils.formatMinutes;

public class StatisticsFragment extends Fragment {

    //TODO: move to separate file
    private class Stats {
        long today;
        long thisWeek;
        long thisMonth;
        long total;

        Stats(long today, long thisWeek, long thisMonth, long total) {
            this.today = today;
            this.thisWeek = thisWeek;
            this.thisMonth = thisMonth;
            this.total = total;
        }

        Stats() {
            this.today = 0;
            this.thisWeek = 0;
            this.thisMonth = 0;
            this.total = 0;
        }
    }

    private LineChart mChart;

    private TextView mStatsToday;
    private TextView mStatsThisWeek;
    private TextView mStatsThisMonth;
    private TextView mStatsTotal;

    private Spinner mStatsTypeSpinner;
    private Spinner mRangeTypeSpinner;

    private CustomXAxisFormatter mXAxisFormatter;

    final private float CHART_TEXT_SIZE = 12f;

    private List<LocalDate> xValues = new ArrayList<>();

    private LinearLayout mLabelCheckboxes;
    private List<Session> mSessionsDataTotal;
    private Map<String, List<Session>> mSessionsData;
    private Stats mStats;

    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        StatisticsMainBinding binding = DataBindingUtil.inflate(
                inflater, R.layout.statistics_main, container, false);
        View view = binding.getRoot();

        mChart = binding.chart;
        mStatsToday = binding.statsToday;
        mStatsThisWeek = binding.statsWeek;
        mStatsThisMonth = binding.statsMonth;
        mStatsTotal = binding.statsTotal;

        mStatsTypeSpinner = binding.statsTypeSpinner;
        mRangeTypeSpinner = binding.rangeTypeSpinner;
        mLabelCheckboxes = binding.labelCheckboxes;

        binding.allEntriesButton.setOnClickListener(view12 -> {
            Intent intent = new Intent(getActivity(), AllEntriesActivity.class);
            startActivity(intent);
        });

        binding.backupButton.setOnClickListener(view1 -> {
            //TODO: clean-up
            AppDatabase.closeInstance();
            File file = getContext().getDatabasePath("goodtime-db");
            File destinationPath = new File(getContext().getFilesDir(), "tmp");
            File destinationFile = new File(destinationPath, "Goodtime-Backup-" + formatDateAndTime(System.currentTimeMillis()));

            Runnable r = () -> {
                if (file.exists()) {
                    try {
                        copyFile(file, destinationFile);
                        if (destinationFile.exists()) {
                            Uri fileUri = FileProvider.getUriForFile(getContext(), "com.apps.adrcotfas.goodtime", destinationFile);
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_SEND);
                            intent.setType("application/zip");
                            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(intent, "ceva fin"));
                        } else {
                            Toast.makeText(getContext(), "Dataabase invalid", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IOException e) {
                        // TODO: autogenerated
                        e.printStackTrace();
                    }
                }
            };

            Thread t = new Thread(r);
            t.start();
        });

        binding.importButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //TODO: warn the user about loosing the data
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                //TODO: add request code
                startActivityForResult(intent, 123);
            }
        });


        setupLabelCheckboxes();
        setupSpinners();
        setupChart();
        setupSessionsObserver();
        return view;
    }

    private void setupLabelCheckboxes() {
        AppDatabase.getDatabase(getActivity().getApplicationContext()).labelAndColor().getLabels().observe(this, labels -> {
            for (LabelAndColor label : labels) {
                CheckBox checkBox = new CheckBox(getActivity());
                checkBox.setText(label.label);
                checkBox.setTextColor(label.color);
                mLabelCheckboxes.addView(checkBox);
            }

            for (int i = 0; i < mLabelCheckboxes.getChildCount(); ++i) {
                CheckBox c = (CheckBox) mLabelCheckboxes.getChildAt(i);
                c.setOnClickListener(view -> {
                    if (c.isChecked()) {
                        LiveData<List<Session>> sessions =
                                AppDatabase.getDatabase(getActivity().getApplicationContext())
                                        .sessionModel().getSessions(c.getText().toString());
                        sessions.observe(this, sessions1 -> {
                            //TODO: observe and add to mSessionsData
                        });


                    } else {
                        //TODO: remove from mSessionsData
                    }
                    //refreshStats();
                    //TODO: refresh stats
                    //TODO: refresh graph
                });
            }
        });
    }

    private void accumulateStatsForUi(List<Session> sessions) {
        final boolean isDurationType = mStatsTypeSpinner.getSelectedItemPosition() == DURATION.ordinal();

        final LocalDate today          = new LocalDate();
        final LocalDate thisWeekStart  = today.dayOfWeek().withMinimumValue().minusDays(1);
        final LocalDate thisWeekEnd    = today.dayOfWeek().withMaximumValue().plusDays(1);
        final LocalDate thisMonthStart = today.dayOfMonth().withMinimumValue().minusDays(1);
        final LocalDate thisMonthEnd   = today.dayOfMonth().withMaximumValue().plusDays(1);

        for (Session s : sessions) {
            final long increment = isDurationType ? s.totalTime : 1;

            final LocalDate crt = new LocalDate(new Date(s.endTime));
            if (crt.isEqual(today)) {
                mStats.today += increment;
            }
            if (crt.isAfter(thisWeekStart) && crt.isBefore(thisWeekEnd)) {
                mStats.thisWeek += increment;
            }
            if (crt.isAfter(thisMonthStart) && crt.isBefore(thisMonthEnd)) {
                mStats.thisMonth += increment;
            }
            if (isDurationType) {
                mStats.total += increment;
            }
        }
        if (!isDurationType) {
            mStats.total += sessions.size();
        }
    }

    private void refreshStats() {
        final boolean isDurationType = mStatsTypeSpinner.getSelectedItemPosition() == DURATION.ordinal();

        mStats = new Stats();

        //TODO: if "total" checkbox is checked
        if (true) {
            accumulateStatsForUi(mSessionsDataTotal);
        } else {
            for(Map.Entry<String, List<Session>> sessions : mSessionsData.entrySet()) {
                accumulateStatsForUi(sessions.getValue());
            }
        }

        mStatsToday.setText(isDurationType || mStats.today == 0
                ? formatMinutes(mStats.today)
                : Long.toString(mStats.today));
        mStatsThisWeek.setText(isDurationType || mStats.thisWeek == 0
                ? formatMinutes(mStats.thisWeek)
                : Long.toString(mStats.thisWeek));
        mStatsThisMonth.setText(isDurationType || mStats.thisMonth == 0
                ? formatMinutes(mStats.thisMonth)
                : Long.toString(mStats.thisMonth));
        mStatsTotal.setText(isDurationType || mStats.total == 0 ?
                formatMinutes(mStats.total)
                : Long.toString(mStats.total));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 123) {
            //TODO: verify if the file is valid, and only after that copy
            //TODO: after the copy is done, call setupSessionsObserver to reset the graph
            //TODO: clean-up
            if (resultCode == RESULT_OK) {
                try {
                    InputStream inputStream = getActivity().getContentResolver().openInputStream(data.getData());
                    AppDatabase.destroyInstance();
                    File destinationPath = getContext().getDatabasePath("goodtime-db");
                    //TODO: copy should be done on a background thread
                    copy(inputStream, destinationPath);
                    //TODO: refresh checkboxes (labels were probably changed)
                    setupSessionsObserver();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //TODO: move these copy functions to an utility class
    private static void copyFile(File sourceFile, File destFile) throws IOException {
        if (!destFile.getParentFile().exists())
            destFile.getParentFile().mkdirs();

        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel source = new FileInputStream(sourceFile).getChannel(); FileChannel destination = new FileOutputStream(destFile).getChannel()) {
            destination.transferFrom(source, 0, source.size());
        }
    }

    public static void copy(InputStream inStream, File dst) throws IOException
    {
        FileOutputStream outStream = new FileOutputStream(dst);
        copy(inStream, outStream);
    }

    public static void copy(InputStream in, OutputStream out) throws IOException
    {
        int numBytes;
        byte[] buffer = new byte[1024];

        while ((numBytes = in.read(buffer)) != -1)
            out.write(buffer, 0, numBytes);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> statsTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_stats_type, android.R.layout.simple_spinner_item);
        statsTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mStatsTypeSpinner.setAdapter(statsTypeAdapter);

        mStatsTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                setupSessionsObserver();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        ArrayAdapter < CharSequence > rangeTypeAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_range_type, android.R.layout.simple_spinner_item);
        rangeTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mRangeTypeSpinner.setAdapter(rangeTypeAdapter);
        mRangeTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mXAxisFormatter.setRangeType(SpinnerRangeType.values()[i]);
                setupSessionsObserver();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void setupSessionsObserver() {
        AppDatabase.getDatabase(getActivity().getApplicationContext()).sessionModel().getAllSessionsByEndTime()
                .observe(this, sessions -> {

            final boolean isDurationType = mStatsTypeSpinner.getSelectedItemPosition() == DURATION.ordinal();

            mSessionsDataTotal = sessions;
            refreshStats();

            final LineData data = generateChartData(sessions);

            mChart.moveViewToX(data.getXMax());
            mChart.setData(data);
            mChart.getData().setHighlightEnabled(false);

            mChart.getAxisLeft().setAxisMinimum(0f);
            mChart.getAxisLeft().setAxisMaximum(isDurationType ? 60f : 6f);

            final int visibleXRange = pxToDp(mChart.getWidth()) / 46;
            mChart.setVisibleXRangeMaximum(visibleXRange);
            mChart.setVisibleXRangeMinimum(visibleXRange);
            mChart.getXAxis().setLabelCount(visibleXRange);

            if (sessions.size() > 0 && data.getYMax() >= (isDurationType ? 60 : 6f)) {
                mChart.getAxisLeft().resetAxisMaximum();
            }

            mChart.notifyDataSetChanged();
        });
    }

    private void setupChart() {
        mChart.setXAxisRenderer(new CustomXAxisRenderer(
                mChart.getViewPortHandler(),
                mChart.getXAxis(),
                mChart.getTransformer(YAxis.AxisDependency.LEFT)));

        mChart.setRendererLeftYAxis(new CustomYAxisRenderer(
                mChart.getViewPortHandler(),
                mChart.getAxisLeft(),
                mChart.getTransformer(YAxis.AxisDependency.LEFT)
                ));

        YAxis yAxis = mChart.getAxisLeft();
        yAxis.setTextColor(getActivity().getResources().getColor(R.color.white));
        yAxis.setGranularity(1);
        yAxis.setTextSize(CHART_TEXT_SIZE);
        yAxis.setDrawAxisLine(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setGranularityEnabled(true);
        xAxis.setTextColor(getActivity().getResources().getColor(R.color.white));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        final SpinnerRangeType rangeType =
                SpinnerRangeType.values()[mRangeTypeSpinner.getSelectedItemPosition()];

        mXAxisFormatter = new CustomXAxisFormatter(xValues, rangeType);
        xAxis.setValueFormatter(mXAxisFormatter);
        xAxis.setAvoidFirstLastClipping(false);
        xAxis.setTextSize(CHART_TEXT_SIZE);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);

        mChart.setExtraTopOffset(30f);
        mChart.setExtraBottomOffset(20f);
        mChart.setExtraLeftOffset(-30);
        mChart.getAxisRight().setEnabled(false);
        mChart.getDescription().setEnabled(false);
        mChart.setNoDataText("");
        mChart.setHardwareAccelerationEnabled(true);
        mChart.animateY(500, Easing.EasingOption.EaseOutCubic);
        mChart.getLegend().setEnabled(false);
        mChart.setPinchZoom(false);
        mChart.setDoubleTapToZoomEnabled(false);
        mChart.setScaleEnabled(true);
        mChart.setDragEnabled(true);
        mChart.invalidate();
        mChart.notifyDataSetChanged();
    }

    private LineData generateChartData(List<Session> sessions) {

        final SpinnerStatsType statsType =
                SpinnerStatsType.values()[mStatsTypeSpinner.getSelectedItemPosition()];
        final SpinnerRangeType rangeType =
                SpinnerRangeType.values()[mRangeTypeSpinner.getSelectedItemPosition()];

        final int DUMMY_INTERVAL_RANGE = 15;

        List<Entry> yVals = new ArrayList<>();
        TreeMap<LocalDate, Long> tree = new TreeMap<>();

        // generate dummy data
        LocalDate dummyEnd = new LocalDate().plusDays(1);
        switch (rangeType) {
            case DAYS:
                LocalDate dummyBegin = dummyEnd.minusDays(DUMMY_INTERVAL_RANGE);
                for (LocalDate i = dummyBegin; i.isBefore(dummyEnd); i = i.plusDays(1)) {
                    tree.put(i, 0L);
                }
                break;
            case WEEKS:
                dummyBegin = dummyEnd.minusWeeks(DUMMY_INTERVAL_RANGE).dayOfWeek().withMinimumValue();
                for (LocalDate i = dummyBegin; i.isBefore(dummyEnd); i = i.plusWeeks(1)) {
                    tree.put(i, 0L);
                }
                break;
            case MONTHS:
                dummyBegin = dummyEnd.minusMonths(DUMMY_INTERVAL_RANGE);
                for (LocalDate i = dummyBegin; i.isBefore(dummyEnd); i = i.plusMonths(1)) {
                    tree.put(i, 0L);
                }
                break;
        }

        // this is to sum up entries from the same day for visualization
        for (int i = 0; i < sessions.size(); ++i) {
            LocalDate localTime = new LocalDate();
            switch (rangeType) {
                case DAYS:
                    localTime = new LocalDate(new Date(sessions.get(i).endTime));
                    break;
                case WEEKS:
                    localTime = new LocalDate(new Date(sessions.get(i).endTime)).dayOfWeek().withMinimumValue();
                    break;
                case MONTHS:
                    localTime = new LocalDate(new Date(sessions.get(i).endTime)).dayOfMonth().withMinimumValue();
                    break;
            }

            if (!tree.containsKey(localTime)) {
                tree.put(localTime, statsType == DURATION ? sessions.get(i).totalTime : 1);
            } else {
                tree.put(localTime, tree.get(localTime)
                        + (statsType == DURATION ? sessions.get(i).totalTime : 1));
            }
        }

        if (tree.size() > 0) {
            xValues.clear();
            int i = 0;
            LocalDate previousTime = tree.firstKey();

            for (LocalDate crt : tree.keySet()) {
                // visualize intermediate days/weeks/months in case of days without completed sessions
                LocalDate beforeWhat = new LocalDate();
                switch (rangeType) {
                    case DAYS:
                        beforeWhat = crt.minusDays(1);
                        break;
                    case WEEKS:
                        beforeWhat = crt.minusWeeks(1);
                        break;
                    case MONTHS:
                        beforeWhat = crt.minusMonths(1);
                }

                while(previousTime.isBefore(beforeWhat)) {
                    yVals.add(new Entry(i, 0));

                    switch (rangeType) {
                        case DAYS:
                            previousTime = previousTime.plusDays(1);
                            break;
                        case WEEKS:
                            previousTime = previousTime.plusWeeks(1);
                            break;
                        case MONTHS:
                            previousTime = previousTime.plusMonths(1);
                    }
                    xValues.add(previousTime);
                    ++i;
                }
                yVals.add(new Entry(i, tree.get(crt)));
                xValues.add(crt);
                ++i;
                previousTime = crt;
            }
        }
        return new LineData(generateLineDataSet(yVals, getActivity().getResources().getColor(R.color.cyan)));
    }

    private LineDataSet generateLineDataSet(List<Entry> entries, int color) {
        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(color);
        set.setLineWidth(3f);
        set.setCircleRadius(3f);
        set.setDrawCircleHole(false);
        set.disableDashedLine();
        set.setDrawFilled(true);
        set.setDrawValues(false);

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            set.setDrawFilled(false);
            set.setLineWidth(2f);
            set.setCircleSize(4f);
            set.setDrawCircleHole(true);
        }
        return set;
    }

    public int pxToDp(int px) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        int dp = Math.round(px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return dp;
    }
}
