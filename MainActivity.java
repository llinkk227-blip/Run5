package com.example.runintervals;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.*;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polyline;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ=10; final int BG=Color.rgb(247,248,250), DARK=Color.rgb(30,38,43), TEAL=Color.rgb(22,160,133);
    LinearLayout root, content; TextView title,timeTv,distTv,paceTv,avgTv,gpsTv; MapView map; Polyline route;
    FusedLocationProviderClient locationClient; LocationCallback locationCallback; SharedPreferences prefs;
    ArrayList<GeoPoint> points=new ArrayList<>(); Handler handler=new Handler(); long elapsed,lastTick; double distance,weight=60; boolean running,paused;

    @Override public void onCreate(Bundle b){super.onCreate(b); Configuration.getInstance().setUserAgentValue(getPackageName()); prefs=getSharedPreferences("runs",MODE_PRIVATE); weight=Double.longBitsToDouble(prefs.getLong("weight",Double.doubleToLongBits(60))); locationClient=LocationServices.getFusedLocationProviderClient(this); shell(); showRun();}
    TextView text(String s,float z){TextView v=new TextView(this);v.setText(s);v.setTextSize(z);v.setTextColor(DARK);v.setPadding(10,8,10,8);return v;}
    Button button(String s){Button b=new Button(this);b.setText(s);b.setTextSize(14);b.setAllCaps(false);return b;}
    void shell(){root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);TextView menu=text("☰",30);title=text("Пробежка",21);title.setTypeface(null,1);top.addView(menu,new LinearLayout.LayoutParams(64,64));top.addView(title);root.addView(top);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(14,4,14,10);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);menu.setOnClickListener(v->drawer());}
    void drawer(){final PopupWindow pw=new PopupWindow(this);LinearLayout d=new LinearLayout(this);d.setOrientation(LinearLayout.VERTICAL);d.setPadding(25,30,25,20);d.setBackgroundColor(Color.WHITE);TextView h=text("RunIntervals",26);h.setTypeface(null,1);h.setTextColor(TEAL);d.addView(h);TextView s=text("Беговой трекер",14);s.setTextColor(Color.GRAY);d.addView(s);String[] items={"🏃  Пробежка","📋  История","📊  Статистика","⚙  Настройки"};for(String x:items){Button b=button(x);d.addView(b);if(x.contains("Пробежка"))b.setOnClickListener(v->{pw.dismiss();showRun();});if(x.contains("История"))b.setOnClickListener(v->{pw.dismiss();showHistory();});if(x.contains("Статистика"))b.setOnClickListener(v->{pw.dismiss();showStats();});if(x.contains("Настройки"))b.setOnClickListener(v->{pw.dismiss();showSettings();});}pw.setContentView(d);pw.setWidth((int)(getResources().getDisplayMetrics().widthPixels*.82));pw.setHeight(-1);pw.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.WHITE));pw.setOutsideTouchable(true);pw.setFocusable(true);pw.showAtLocation(root,Gravity.LEFT|Gravity.TOP,0,0);}
    LinearLayout cell(String l,TextView v){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(6,6,6,6);TextView a=text(l,12);a.setTextColor(Color.GRAY);c.addView(a);c.addView(v);return c;}
    void showRun(){title.setText("Пробежка");content.removeAllViews();LinearLayout m=new LinearLayout(this);timeTv=text("00:00:00",25);distTv=text("0,00 км",25);paceTv=text("--:--",25);avgTv=text("--:--",25);for(TextView v:new TextView[]{timeTv,distTv,paceTv,avgTv})v.setTypeface(null,1);m.addView(cell("Время",timeTv),new LinearLayout.LayoutParams(0,-2,1));m.addView(cell("Дистанция",distTv),new LinearLayout.LayoutParams(0,-2,1));m.addView(cell("Темп",paceTv),new LinearLayout.LayoutParams(0,-2,1));m.addView(cell("Средний",avgTv),new LinearLayout.LayoutParams(0,-2,1));content.addView(m);gpsTv=text("● GPS: ожидание",13);gpsTv.setTextColor(Color.GRAY);content.addView(gpsTv);map=new MapView(this);map.setTileSource(TileSourceFactory.MAPNIK);map.setMultiTouchControls(true);map.getController().setZoom(14);map.getController().setCenter(new GeoPoint(44.8176,20.4633));content.addView(map,new LinearLayout.LayoutParams(-1,0,1));LinearLayout bs=new LinearLayout(this);Button st=button("▶ Старт"),pa=button("⏸ Пауза"),fi=button("■ Завершить");bs.addView(st,new LinearLayout.LayoutParams(0,58,1));bs.addView(pa,new LinearLayout.LayoutParams(0,58,1));bs.addView(fi,new LinearLayout.LayoutParams(0,58,1));content.addView(bs);st.setOnClickListener(v->start());pa.setOnClickListener(v->pause());fi.setOnClickListener(v->finishRun());}
    void start(){if(running&&!paused)return;if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},REQ);return;}if(!paused){elapsed=0;distance=0;points.clear();route=new Polyline();route.setColor(TEAL);route.setWidth(8);map.getOverlays().add(route);}running=true;paused=false;lastTick=System.currentTimeMillis();gpsTv.setText("● GPS: поиск...");LocationRequest r=new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY,1000).setMinUpdateDistanceMeters(3).build();locationCallback=new LocationCallback(){public void onLocationResult(LocationResult lr){for(Location l:lr.getLocations())add(l);}};locationClient.requestLocationUpdates(r,locationCallback,getMainLooper());tick();}
    void add(Location l){if(!running||paused)return;GeoPoint p=new GeoPoint(l.getLatitude(),l.getLongitude());if(!points.isEmpty()){GeoPoint q=points.get(points.size()-1);Location old=new Location("");old.setLatitude(q.getLatitude());old.setLongitude(q.getLongitude());float d=old.distanceTo(l);if(d>=2&&d<100)distance+=d;}points.add(p);route.setPoints(points);map.getController().animateTo(p);map.invalidate();gpsTv.setText("● GPS: работает");metrics();}
    void tick(){if(!running)return;long now=System.currentTimeMillis();elapsed+=(now-lastTick)/1000;lastTick=now;timeTv.setText(fmt(elapsed));metrics();handler.postDelayed(this::tick,1000);}
    void pause(){if(!running)return;if(!paused){paused=true;locationClient.removeLocationUpdates(locationCallback);gpsTv.setText("● GPS: пауза");}else start();}
    void metrics(){double km=distance/1000;distTv.setText(String.format(Locale.US,"%.2f км",km));if(km>.05&&elapsed>0){double p=(elapsed/60)/km;paceTv.setText(pace(p));avgTv.setText(pace(p));}}
    String pace(double x){int m=(int)x,s=(int)Math.round((x-m)*60);if(s==60){m++;s=0;}return String.format(Locale.US,"%d:%02d",m,s);}
    String fmt(long s){return String.format(Locale.US,"%02d:%02d:%02d",s/3600,(s%3600)/60,s%60);}
    int kcal(){return (int)Math.round(weight*.9*(distance/1000));}
    void finishRun(){if(elapsed==0)return;if(locationCallback!=null)locationClient.removeLocationUpdates(locationCallback);running=false;paused=false;String date=new SimpleDateFormat("dd.MM.yyyy HH:mm",Locale.getDefault()).format(new Date());String val=date+"|"+elapsed+"|"+distance+"|"+kcal();prefs.edit().putString("run_"+System.currentTimeMillis(),val).apply();showResult();}
    void showResult(){title.setText("Результат");content.removeAllViews();TextView h=text("Пробежка завершена",26);h.setTypeface(null,1);content.addView(h);result("Время",fmt(elapsed));result("Дистанция",String.format(Locale.US,"%.2f км",distance/1000));result("Средний темп",distance>50?pace((elapsed/60)/(distance/1000)):"--:--");result("Калории",kcal()+" ккал");Button b=button("🏃 Новая пробежка");b.setOnClickListener(v->showRun());content.addView(b);}
    void result(String a,String b){TextView v=text(a+"\n"+b,21);v.setPadding(18,18,18,18);content.addView(v);}
    void showHistory(){title.setText("История");content.removeAllViews();boolean any=false;for(Map.Entry<String,?> e:prefs.getAll().entrySet())if(e.getKey().startsWith("run_")){any=true;String[] x=e.getValue().toString().split("\\|");TextView v=text("🏃 "+x[0]+"\n"+String.format(Locale.US,"%.2f км  •  %s  •  %s ккал",Double.parseDouble(x[2])/1000,fmt(Long.parseLong(x[1])),x[3]),17);v.setPadding(8,16,8,16);content.addView(v);}if(!any)content.addView(text("Пробежек пока нет.",18));}
    void showStats(){title.setText("Статистика");content.removeAllViews();double km=0;long sec=0;int count=0;for(Map.Entry<String,?> e:prefs.getAll().entrySet())if(e.getKey().startsWith("run_")){String[] x=e.getValue().toString().split("\\|");km+=Double.parseDouble(x[2])/1000;sec+=Long.parseLong(x[1]);count++;}result("Пробежек",String.valueOf(count));result("Всего километров",String.format(Locale.US,"%.2f км",km));result("Общее время",fmt(sec));}
    void showSettings(){title.setText("Настройки");content.removeAllViews();content.addView(text("Параметры",24));EditText w=new EditText(this);w.setHint("Вес, кг");w.setInputType(2);w.setText(String.valueOf((int)weight));content.addView(w);Button save=button("Сохранить");save.setOnClickListener(v->{try{weight=Double.parseDouble(w.getText().toString());prefs.edit().putLong("weight",Double.doubleToLongBits(weight)).apply();Toast.makeText(this,"Сохранено",Toast.LENGTH_SHORT).show();}catch(Exception e){Toast.makeText(this,"Введите вес",Toast.LENGTH_SHORT).show();}});content.addView(save);content.addView(text("\nКарта: OpenStreetMap\nДля GPS разреши приложению доступ к местоположению.",15));}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==REQ&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)start();else Toast.makeText(this,"Нужен доступ к местоположению",Toast.LENGTH_LONG).show();}
    @Override protected void onDestroy(){super.onDestroy();if(locationCallback!=null)locationClient.removeLocationUpdates(locationCallback);if(map!=null)map.onDetach();}
}
