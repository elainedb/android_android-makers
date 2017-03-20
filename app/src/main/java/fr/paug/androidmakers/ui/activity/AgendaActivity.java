package fr.paug.androidmakers.ui.activity;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import fr.paug.androidmakers.R;
import fr.paug.androidmakers.manager.AgendaRepository;
import fr.paug.androidmakers.model.ScheduleSlot;
import fr.paug.androidmakers.model.Session;
import fr.paug.androidmakers.ui.adapter.AgendaPagerAdapter;
import fr.paug.androidmakers.ui.view.AgendaView;

public class AgendaActivity extends AppCompatActivity implements AgendaView.AgendaClickListener {

    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_agenda);

        mViewPager = (ViewPager) findViewById(R.id.viewpager);

        AgendaRepository.getInstance().load(new AgendaLoadListener(this));
    }

    @Override
    public void onClick(AgendaView.Item agendaItem) {
        DetailActivity.startActivity(this, agendaItem);
    }

    private void onAgendaLoaded() {
        List<ScheduleSlot> scheduleSlots = AgendaRepository.getInstance().getScheduleSlots();

        SparseArray<AgendaView.Items> itemByDayOfTheYear = new SparseArray<>();

        Calendar calendar = Calendar.getInstance();
        for (ScheduleSlot scheduleSlot : scheduleSlots) {
            List<AgendaView.Item> agendaItems = getAgendaItems(
                    itemByDayOfTheYear, calendar, scheduleSlot);
            agendaItems.add(new AgendaView.Item(scheduleSlot, getTitle(scheduleSlot.sessionId)));
        }

        List<AgendaView.Items> items = getItemsOrdered(itemByDayOfTheYear);
        PagerAdapter adapter = new AgendaPagerAdapter(items, this);
        mViewPager.setAdapter(adapter);
    }

    @NonNull
    private List<AgendaView.Item> getAgendaItems(SparseArray<AgendaView.Items> itemByDayOfTheYear,
                                                 Calendar calendar, ScheduleSlot scheduleSlot) {
        SparseArray<List<AgendaView.Item>> itemsSparseArray =
                getItemsByRoomList(itemByDayOfTheYear, calendar, scheduleSlot);
        List<AgendaView.Item> agendaItems = itemsSparseArray.get(scheduleSlot.room);
        if (agendaItems == null) {
            agendaItems = new ArrayList<>();
            itemsSparseArray.put(scheduleSlot.room, agendaItems);
        }
        return agendaItems;
    }

    private SparseArray<List<AgendaView.Item>> getItemsByRoomList(
            SparseArray<AgendaView.Items> itemByDayOfTheYear,
            Calendar calendar, ScheduleSlot scheduleSlot) {

        calendar.setTimeInMillis(scheduleSlot.startDate);
        int dayIndex = calendar.get(Calendar.DAY_OF_YEAR) + calendar.get(Calendar.YEAR) * 1000;
        AgendaView.Items items = itemByDayOfTheYear.get(dayIndex);
        SparseArray<List<AgendaView.Item>> itemsSparseArray;
        if (items == null) {
            itemsSparseArray = new SparseArray<>();
            String title = DateFormat.getDateInstance().format(calendar.getTime());
            items = new AgendaView.Items(title, itemsSparseArray);
            itemByDayOfTheYear.put(dayIndex, items);
            return itemsSparseArray;
        } else {
            return items.getItems();
        }
    }

    @NonNull
    private List<AgendaView.Items> getItemsOrdered(
            SparseArray<AgendaView.Items> itemByDayOfTheYear) {
        int size = itemByDayOfTheYear.size();
        int[] keysSorted = new int[size];
        for (int i = 0; i < size; i++) {
            keysSorted[i] = itemByDayOfTheYear.keyAt(i);
        }
        Arrays.sort(keysSorted);
        List<AgendaView.Items> items = new ArrayList<>(size);
        for (int key : keysSorted) {
            items.add(itemByDayOfTheYear.get(key));
        }
        return items;
    }

    private String getTitle(int sessionId) {
        Session session = AgendaRepository.getInstance().getSession(sessionId);
        return session == null ? "?" : session.title;
    }

    private static class AgendaLoadListener implements AgendaRepository.OnLoadListener {
        private WeakReference<AgendaActivity> mAgendaActivity;

        private AgendaLoadListener(AgendaActivity agendaActivity) {
            mAgendaActivity = new WeakReference<>(agendaActivity);
        }

        @Override
        public void onAgendaLoaded() {
            AgendaActivity agendaActivity = mAgendaActivity.get();
            if (agendaActivity == null) {
                return;
            }
            agendaActivity.onAgendaLoaded();
        }
    }
}
