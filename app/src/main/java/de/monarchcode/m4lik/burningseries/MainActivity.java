package de.monarchcode.m4lik.burningseries;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.monarchcode.m4lik.burningseries.api.API;
import de.monarchcode.m4lik.burningseries.api.APIInterface;
import de.monarchcode.m4lik.burningseries.database.MainDBHelper;
import de.monarchcode.m4lik.burningseries.mainFragments.FavsFragment;
import de.monarchcode.m4lik.burningseries.mainFragments.GenresFragment;
import de.monarchcode.m4lik.burningseries.mainFragments.SeriesFragment;
import de.monarchcode.m4lik.burningseries.objects.GenreMap;
import de.monarchcode.m4lik.burningseries.objects.GenreObj;
import de.monarchcode.m4lik.burningseries.objects.ShowObj;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static de.monarchcode.m4lik.burningseries.database.SeriesContract.SQL_TRUNCATE_GENRES_TABLE;
import static de.monarchcode.m4lik.burningseries.database.SeriesContract.SQL_TRUNCATE_SERIES_TABLE;
import static de.monarchcode.m4lik.burningseries.database.SeriesContract.genresTable;
import static de.monarchcode.m4lik.burningseries.database.SeriesContract.seriesTable;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    public static String userName;
    public static String userSession;

    public static Menu menu;

    public String visibleFragment;
    public Boolean seriesList = false;

    Boolean loaded = false;

    Boolean seriesDone;
    Boolean favsDone;

    MainDBHelper dbHelper;
    SQLiteDatabase database;

    NavigationView navigationView = null;
    Toolbar toolbar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);

        favsDone = false;
        seriesDone = false;

        SharedPreferences sharedPreferences = getSharedPreferences(
                "de.monarchcode.m4lik.burningseries.LOGIN",
                Context.MODE_PRIVATE
        );
        userSession = sharedPreferences.getString("session", "");
        userName = sharedPreferences.getString("user", "Bitte Einloggen");


        dbHelper = new MainDBHelper(getApplicationContext());
        database = dbHelper.getWritableDatabase();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        toggle.syncState();

        TextView userTextView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.nav_username_text);
        userTextView.setText(userName);

        if (userSession.equals("")) {
            navigationView.getMenu().findItem(R.id.login_menu_item).setVisible(true);
            navigationView.getMenu().findItem(R.id.logout_menu_item).setVisible(false);
        } else {
            navigationView.getMenu().findItem(R.id.login_menu_item).setVisible(false);
            navigationView.getMenu().findItem(R.id.logout_menu_item).setVisible(true);
        }
    }

    @Override
    public void onBackPressed() {
        Boolean dosuper = true;

        if (seriesList) {
            setFragment("genresFragment");
            seriesList = false;
            dosuper = false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (!drawer.isDrawerOpen(GravityCompat.START) && dosuper) {
            drawer.openDrawer(GravityCompat.START);
            dosuper = false;
        }

        if (dosuper)
            super.onBackPressed();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        this.menu = menu;

        MainDBHelper dbhelper = new MainDBHelper(getApplicationContext());
        SQLiteDatabase db  = dbhelper.getReadableDatabase();

        Cursor c = db.query(
                seriesTable.TABLE_NAME,
                new String[]{seriesTable.COLUMN_NAME_ID},
                null,
                null,
                null,
                null,
                null
        );

        if (c.getCount() == 0)
            updateDatabase("seriesFragment");
        else
            setFragment("seriesFragment");

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            seriesDone = false;
            favsDone = false;
            switch (visibleFragment) {
                case "genresFragment":
                    updateDatabase("genresFragment");
                    break;
                case "favsFragment":
                    updateDatabase("favsFragment");
                    break;
                case "seriesFragment":
                    updateDatabase("seriesFragment");
                    break;
                default:
                    updateDatabase("seriesFragment");
                    break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Intent intent;

        switch (item.getItemId()) {

            case R.id.nav_series:
                setFragment("seriesFragment");
                break;

            case R.id.nav_genres:
                setFragment("genresFragment");
                break;

            case R.id.nav_favs:
                setFragment("favsFragment");
                break;

            case R.id.login_menu_item:
                intent = new Intent(this, LoginActivity.class);
                startActivity(intent);
                break;

            case R.id.logout_menu_item:
                logout();
                break;

            case R.id.nav_share:
                break;

            case R.id.nav_settings:
                /*intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);*/
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void logout() {
        final SharedPreferences sharedPreferences = getSharedPreferences(
                "de.monarchcode.m4lik.burningseries.LOGIN",
                Context.MODE_PRIVATE
        );
        final API api = new API();
        api.setSession(sharedPreferences.getString("session", ""));
        api.generateToken("logout");


        APIInterface apii = api.getApiInterface();
        Call<ResponseBody> call = apii.logout(api.getToken(), api.getUserAgent(), api.getSession());
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                sharedPreferences.edit().clear().apply();

                navigationView.getMenu().findItem(R.id.logout_menu_item).setVisible(false);
                navigationView.getMenu().findItem(R.id.login_menu_item).setVisible(true);

                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Ausgeloggt", Snackbar.LENGTH_SHORT);
                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                snackbar.show();
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Snackbar.make(findViewById(android.R.id.content), "Verbindungsfehler.", Snackbar.LENGTH_SHORT).show();
            }
        });

    }

    private void setFragment(String fragment) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        MenuItem searchItem = MainActivity.getMenu().findItem(R.id.action_search);

        switch (fragment) {
            case "genresFragment":
                searchItem.setVisible(false);
                transaction.replace(R.id.fragmentContainerMain, new GenresFragment());
                transaction.commit();
                visibleFragment = "genresFragment";
                break;
            case "favsFragment":
                searchItem.setVisible(true);
                transaction.replace(R.id.fragmentContainerMain, new FavsFragment());
                transaction.commit();
                visibleFragment = "favsFragment";
                break;
            default:
                searchItem.setVisible(true);
                transaction.replace(R.id.fragmentContainerMain, new SeriesFragment());
                transaction.commit();
                visibleFragment = "seriesFragment";
                break;
        }
    }

    private void updateDatabase(final String fragment) {

        MainDBHelper dbHelper = new MainDBHelper(getApplicationContext());
        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.execSQL(SQL_TRUNCATE_SERIES_TABLE);

        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setTitle("Serien werden geladen.\nBitte kurz warten...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        API api = new API();
        APIInterface apiInterface = api.getApiInterface();
        api.setSession(MainActivity.userSession);
        api.generateToken("series:genre");
        Call<GenreMap> call = apiInterface.getSeriesGenreList(api.getToken(), api.getUserAgent(), api.getSession());
        call.enqueue(new Callback<GenreMap>() {
            @Override
            public void onResponse(Call<GenreMap> call, Response<GenreMap> response) {

                GenreMap map = response.body();

                db.execSQL(SQL_TRUNCATE_GENRES_TABLE);

                int genreID = 0;
                for (Map.Entry<String, GenreObj> entry : map.entrySet()) {
                    String currentGenre = entry.getKey();
                    GenreObj go = entry.getValue();
                    ContentValues values = new ContentValues();
                    values.put(genresTable.COLUMN_NAME_GENRE, currentGenre);
                    values.put(genresTable.COLUMN_NAME_ID, genreID);
                    db.insert(genresTable.TABLE_NAME, null, values);
                    Iterator itr = Arrays.asList(go.getShows()).iterator();
                    int i = 0;
                    int all = 0;
                    while (i < go.getShows().length) {
                        int j = 1;

                        db.beginTransaction();
                        while (j <= 50 && itr.hasNext()) {
                            ShowObj show = (ShowObj) itr.next();

                            ContentValues cv = new ContentValues();
                            cv.put(seriesTable.COLUMN_NAME_ID, show.getId());
                            cv.put(seriesTable.COLUMN_NAME_TITLE, show.getName());
                            cv.put(seriesTable.COLUMN_NAME_GENRE, currentGenre);
                            cv.put(seriesTable.COLUMN_NAME_DESCR, "");
                            cv.put(seriesTable.COLUMN_NAME_ISFAV, 0);

                            db.insert(seriesTable.TABLE_NAME, null, cv);

                            j++;
                            i++;
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();
                    }
                    genreID++;
                }

                progressDialog.dismiss();

                seriesDone = true;
                setFragmentDelayed(fragment);
            }

            @Override
            public void onFailure(Call<GenreMap> call, Throwable t) {
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Da ist was schief gelaufen...\n" +
                                "Bitte App neustarten.\n\n" +
                                "Sollte der Fehler weiterhin bestehen installiere die App erneut", Snackbar.LENGTH_LONG);
                View snackbarView = snackbar.getView();
                snackbarView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                snackbar.show();

                getApplicationContext().deleteDatabase(MainDBHelper.DATABASE_NAME);
            }
        });


        if (!userSession.equals("")) {
            api.generateToken("user/series");

            Call<List<ShowObj>> favscall = apiInterface.getFavorites(api.getToken(), api.getUserAgent(), api.getSession());
            favscall.enqueue(new Callback<List<ShowObj>>() {
                @Override
                public void onResponse(Call<List<ShowObj>> call, Response<List<ShowObj>> response) {

                    for (ShowObj show : response.body()) {
                        ContentValues cv = new ContentValues();
                        cv.put(seriesTable.COLUMN_NAME_ISFAV, 1);
                        db.update(seriesTable.TABLE_NAME, cv, seriesTable.COLUMN_NAME_ID + " = ?", new String[]{show.getId().toString()});
                    }

                    favsDone = true;
                    setFragmentDelayed(fragment);
                }

                @Override
                public void onFailure(Call<List<ShowObj>> call, Throwable t) {
                    t.printStackTrace();

                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Da ist was schief gelaufen...", Snackbar.LENGTH_SHORT);
                    View snackbarView = snackbar.getView();
                    snackbarView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDark));
                    snackbar.show();
                }
            });
        }
    }

    public void setFragmentDelayed(String fragment) {
        if (!userSession.equals("")) {
            if (favsDone && seriesDone)
                setFragment(fragment);
        } else
            if (seriesDone)
                setFragment(fragment);
    }

    public static Menu getMenu() {
        return menu;
    }
}
