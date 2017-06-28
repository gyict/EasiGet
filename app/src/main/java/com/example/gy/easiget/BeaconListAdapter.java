package com.example.gy.easiget;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.easimote.sdk.Region;
import com.easimote.sdk.beacon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by lenovo on 2017/6/14.
 */

public class BeaconListAdapter extends BaseAdapter{

    private ArrayList<beacon> beacons;
    private LayoutInflater inflater;
    public List<Region> interestedregion = GetpageActivity.interestedRegion;
    public HashMap<Region,String> ruhash = GetpageActivity.ruhash;
    public List<String> webs = GetpageActivity.webs;


    public BeaconListAdapter(Context paramContext)
    {
        this.inflater = LayoutInflater.from(paramContext);
        this.beacons = new ArrayList();
    }


    private void bind(final beacon beacon, View view) {
//        Log.e("name",beacon.getName());
        final ViewHolder holder =  (ViewHolder) view.getTag();

//        holder.nameMessageView.setText("Name:");
//        holder.macMessageView.setText("MAC:");
        String name = beacon.getName();
        if(name==null) name="";
        Region region = GetpageActivity.checkIsInInterestRegion(beacon);
        String url = ruhash.get(region);
        if(url.equals(webs.get(0))){
            holder.nameMessageView.setText("金编钟");
            holder.macMessageView.setText("点击了解更多");
            holder.imageView.setImageResource(R.drawable.jbz);
        }else if(url.equals(webs.get(1))){
            holder.nameMessageView.setText("金瓯永固杯");
            holder.macMessageView.setText("点击了解更多");
            holder.imageView.setImageResource(R.drawable.joygb);
        }

    }

    private View inflateIfRequired(View view, int position, ViewGroup parent) {
        if (view == null) {
            view = inflater.inflate(R.layout.event_item, null);
            view.setTag(new ViewHolder(view));
        }
        return view;
    }

    public int getCount()
    {
        return this.beacons.size();
    }


    public void replaceWith(Collection<beacon> paramCollection)
    {
        this.beacons.clear();
        this.beacons.addAll(paramCollection);
        notifyDataSetChanged();
    }

    public void removesome(Collection<beacon> paramCollection)
    {
        this.beacons.removeAll(paramCollection);

        notifyDataSetChanged();
    }

    public void addsome(Collection<beacon> paramCollection)
    {
        this.beacons.removeAll(paramCollection);
        this.beacons.addAll(paramCollection);
        notifyDataSetChanged();
    }

    public void addone(beacon b)
    {
//        if(!this.beacons.contains(b)){
//            this.beacons.add(b);
//            notifyDataSetChanged();
//        }
        this.beacons.add(b);
        notifyDataSetChanged();

    }

    @Override
    public beacon getItem(int paramInt)
    {
        return this.beacons.get(paramInt);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }


    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflateIfRequired(view, position, parent);
        bind(getItem(position), view);
        return view;
    }



    static class ViewHolder {
        final TextView nameMessageView;
        final TextView macMessageView;
        final ImageView imageView;

        ViewHolder(View view) {
            nameMessageView = (TextView) view.findViewWithTag("beacon_name");
            macMessageView = (TextView) view.findViewWithTag("beacon_mac");
            imageView = (ImageView) view.findViewWithTag("web_image");
        }
    }
}
