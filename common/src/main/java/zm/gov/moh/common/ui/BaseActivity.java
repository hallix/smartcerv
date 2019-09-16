package zm.gov.moh.common.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.AccountHeader;
import com.mikepenz.materialdrawer.AccountHeaderBuilder;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.ProfileDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import zm.gov.moh.common.R;
import zm.gov.moh.core.model.IntentAction;
import zm.gov.moh.core.model.Key;
import zm.gov.moh.core.model.submodule.Module;
import zm.gov.moh.core.repository.database.entity.derived.DrawerProvider;
import zm.gov.moh.core.utils.BaseAndroidViewModel;
import zm.gov.moh.core.utils.BaseApplication;

import static zm.gov.moh.core.model.Key.PERSON_ADDRESS;

public class BaseActivity extends AppCompatActivity {


    protected static final String START_SUBMODULE_KEY = "start_submodule";
    public static final String CLIENT_ID_KEY = "PERSON_ID";
    public static final String JSON_FORM = "JSON_FORM";
    public static final String ACTION_KEY = "ACTION_KEY";
    protected LocalBroadcastManager broadcastManager;
    protected static BaseReceiver baseReceiver;
    protected BaseAndroidViewModel viewModel;
    protected ActionBarDrawerToggle drawerToggle;
    DrawerLayout drawerLayout;
    protected ListView drawerList;
    protected String[] layers;
    protected Drawer drawer;

    Toolbar toolbar;
    ToolBarEventHandler toolBarEventHandler;
    String firstname,lastname,userName;
    AccountHeader provider;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

       drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        if (drawerLayout != null) {
            toolbar = findViewById(R.id.base_toolbar);

            drawerToggle = new BaseActionBarDrawerToggle((Activity) this, drawerLayout, toolbar, 0, 0) {
                public void onDrawerClosed(View view) {
                    getActionBar().setTitle(R.string.app_name);
                }

                public void onDrawerOpened(View drawerView) {
                    getActionBar().setTitle("cool");
                }
            };
            drawerLayout.setDrawerListener(drawerToggle);
        }



        if(baseReceiver == null) {
            baseReceiver = new BaseReceiver();
            broadcastManager = LocalBroadcastManager.getInstance(this);

            IntentFilter intentFilter = new IntentFilter(IntentAction.INSUFFICIENT_IDENTIFIERS_FAILD_REGISTRATION);
            IntentFilter syncIntentFilter = new IntentFilter(IntentAction.REMOTE_SYNC_COMPLETE);

            broadcastManager.registerReceiver(baseReceiver, intentFilter);
            broadcastManager.registerReceiver(baseReceiver, syncIntentFilter);
        }
    }

    public void startModule(Module module, Bundle bundle) {

        Intent intent = new Intent(this, module.getClassInstance());

        intent.putExtras(bundle);
        this.startActivity(intent);
    }

    public void startModule(String moduleName, Bundle bundle) {

        BaseApplication baseApplication = (BaseApplication) getApplication();

        Intent intent = new Intent(this, baseApplication.getModule(moduleName).getClassInstance());

        intent.putExtras(bundle);
        this.startActivity(intent);
    }

    public void startModule(String moduleName) {

        BaseApplication baseApplication = (BaseApplication) getApplication();
        Intent intent = new Intent(this, baseApplication.getModule(moduleName).getClassInstance());
        this.startActivity(intent);
    }

    public void startModule(Module module) {

        this.startActivity(new Intent(this, module.getClassInstance()));
    }

    protected void setFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction =
                fragmentManager.beginTransaction();
        fragmentTransaction.replace(android.R.id.content, fragment);
        fragmentTransaction.commit();
    }

    public ToolBarEventHandler getToolbarHandler(Context context){

        if(toolBarEventHandler == null)
         toolBarEventHandler = new ToolBarEventHandler(context);
        return toolBarEventHandler;
    }

    public BaseAndroidViewModel getViewModel() {
        return viewModel;
    }

    protected void setViewModel(BaseAndroidViewModel viewModel) {
        this.viewModel = viewModel;
        Bundle bundle = getIntent().getExtras();

        if (bundle != null)
            viewModel.setBundle(bundle);
    }

    public void initBundle(Bundle bundle) {

        final long SESSION_LOCATION_ID = getViewModel().getRepository().getDefaultSharePrefrences()
                .getLong(Key.LOCATION_ID, 1);
        final String USER_UUID = getViewModel().getRepository().getDefaultSharePrefrences()
                .getString(Key.AUTHORIZED_USER_UUID, "null");
        bundle.putLong(Key.LOCATION_ID, SESSION_LOCATION_ID);

        Long personId = bundle.getLong(Key.PERSON_ID);


        getViewModel()
                .getRepository()
                .getDatabase()
                .providerUserDao()
                .getAllByUserUuid(USER_UUID)
                .observe(this, providerUser -> {

                    bundle.putLong(Key.PROVIDER_ID, providerUser.getProviderId());
                    bundle.putLong(Key.USER_ID, providerUser.getUserId());
                });

        if (personId != null) {

            getViewModel()
                    .getRepository()
                    .getDatabase()
                    .clientDao()
                    .findById(personId)
                    .observe(this, client -> {


                        bundle.putString(Key.PERSON_GIVEN_NAME, client.getGivenName());
                        bundle.putString(Key.PERSON_FAMILY_NAME,client.getFamilyName());

                        //Added to pass client gender with bundle
                        bundle.putString(Key.PERSON_GENDER, client.getGender());
                        //Added to pass client age with bundle

                        bundle.putString(Key.PERSON_AGE, calculateClientAge(client.getBirthDate()).toString());
                        bundle.putString(Key.PERSON_DOB, client.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));


                    });

            getViewModel()
                    .getRepository()
                    .getDatabase()
                    .personAddressDao()
                    .findByPersonIdObservable(personId)
                    .observe(this, personAddress -> {

                        bundle.putString(PERSON_ADDRESS,(personAddress != null)?personAddress.getAddress1()+" "+personAddress.getCityVillage()+" "+personAddress.getStateProvince():this.getResources().getString(R.string.unknown));

                    });
        }
    }

    public class BaseActionBarDrawerToggle extends ActionBarDrawerToggle {

        public BaseActionBarDrawerToggle(Activity activity, DrawerLayout drawerLayout, Toolbar toolbar, int openDrawerContentDescRes, int closeDrawerContentDescRes) {
            super(activity, drawerLayout, toolbar, openDrawerContentDescRes, closeDrawerContentDescRes);
        }
    }

    //Calculate client age as Integer
    public Integer calculateClientAge(LocalDateTime birthdate) {
        //String.valueOf(LocalDate.now().getYear - client.birthdate.getYear())+`years`}"
        int age = LocalDateTime.now().getYear() - birthdate.getYear();
        return age;
    }

    public void addDrawer(Activity activity)
    {   Long providerId;
        providerId=getViewModel().getRepository().getDefaultSharePrefrences().getLong(Key.PROVIDER_ID,0);

       //setup the header of the navigation bar
      getViewModel().getRepository().getDatabase().providerUserDao().getDrawerUser(providerId).observe(this, new Observer<DrawerProvider>() {
          @Override
          public void onChanged(DrawerProvider drawerProvider) {
              userName=drawerProvider.getUserName();
              lastname=drawerProvider.getLastName();
              firstname=drawerProvider.getFirstName();

              buildDrawer(BaseActivity.this, drawerProvider);

          }
      });



    }


    public void buildDrawer(Activity activity, DrawerProvider drawerProvider){


        PrimaryDrawerItem home=new PrimaryDrawerItem().withIdentifier(1).withName("Home").withIcon(R.drawable.home_icon);
        PrimaryDrawerItem allClients=new PrimaryDrawerItem().withIdentifier(2).withName("Records").withIcon(R.drawable.file_icon);
        PrimaryDrawerItem logOut=new PrimaryDrawerItem().withIdentifier(3).withName("Log out").withIcon(R.drawable.ic_logout);

        provider=new AccountHeaderBuilder().withActivity(activity).addProfiles(new ProfileDrawerItem().withName(drawerProvider.getUserName()).withEmail(drawerProvider.getFirstName()+" "+drawerProvider.getLastName()).withIcon(getResources().getDrawable(R.drawable.ic_openmrs))).build();
        drawer = new DrawerBuilder()
                .withActivity(activity)
                .addDrawerItems(home,allClients,logOut).withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if(drawerItem.getIdentifier()==1)
                        {
                            startModule(BaseApplication.CoreModule.HOME);
                        }else if(drawerItem.getIdentifier()==2)
                        {
                            startModule(BaseApplication.CoreModule.REGISTER);
                        }
                        else if(drawerItem.getIdentifier()==3)
                        {
                            startModule(BaseApplication.CoreModule.BOOTSTRAP);
                        }
                        return  true;
                    }
                }).withAccountHeader(provider).build();
    }

}
