package com.netless.whiteboard.activity;

import com.herewhite.sdk.*;
import com.herewhite.sdk.domain.*;
import com.netless.whiteboard.R;
import com.netless.whiteboard.components.AppliancesTooBar;
import com.netless.whiteboard.components.BroadcastManager;
import com.netless.whiteboard.components.SlidesTable;
import com.netless.whiteboard.dialog.InviteDialog;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.util.HashMap;

public class RoomPageActivity extends AppCompatActivity {

    private boolean didLeave = false;
    private String uuid;
    private String roomToken;

    private WhiteSdk whiteSdk;
    private Room room;
    private SlidesTable slidesTable;

    private AppliancesTooBar appliancesTooBar;
    private BroadcastManager broadcastManager;

    private ProgressBar icoLoading;
    private View panTopBar;
    private View panBottomBar;
    private DrawerLayout panMain;
    private RelativeLayout panSlides;

    private Button btnGoBack;
    private Button btnInvite;
    private Button btnCamera;
    private Button btnReplay;
    private Button btnSlides;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();
        setContentView(R.layout.activity_room_page);

        this.icoLoading = findViewById(R.id.icoLoading);
        this.panTopBar = findViewById(R.id.panTopBar);
        this.panBottomBar = findViewById(R.id.panBottomBar);
        this.panMain = findViewById(R.id.activity_main);
        this.panSlides = findViewById(R.id.panSlides);

        this.btnGoBack = findViewById(R.id.btnGoBack);
        this.btnInvite = findViewById(R.id.btnInvite);
        this.btnCamera = findViewById(R.id.btnCamera);
        this.btnReplay = findViewById(R.id.btnReplay);
        this.btnSlides = findViewById(R.id.btnSlides);

        this.appliancesTooBar = new AppliancesTooBar(new HashMap<String, Button>() {{
            this.put("selector", (Button) findViewById(R.id.btnSelector));
            this.put("pencil", (Button) findViewById(R.id.btnPencil));
            this.put("eraser", (Button) findViewById(R.id.btnEraser));
            this.put("text", (Button) findViewById(R.id.btnText));
            this.put("ellipse", (Button) findViewById(R.id.btnEllipse));
            this.put("rectangle", (Button) findViewById(R.id.btnRectangle));
        }});
        this.broadcastManager = new BroadcastManager(this, this.btnCamera);

        this.icoLoading.getIndeterminateDrawable().setColorFilter(
                        getResources().getColor(R.color.colorGrayBorder),
                        android.graphics.PorterDuff.Mode.SRC_IN);

        this.setButtonsEnable(false);

        WhiteBroadView whiteBroadView = findViewById(R.id.whiteboard);
        WhiteSdkConfiguration configuration = new WhiteSdkConfiguration(DeviceType.touch, 10, 0.1);

        Bundle bundle = this.getIntent().getExtras();

        this.uuid = bundle.getString("uuid");
        this.roomToken = bundle.getString("roomToken");

        RoomParams roomParams = new RoomParams(this.uuid, this.roomToken);

        this.slidesTable = new SlidesTable(this);
        this.whiteSdk = new WhiteSdk(whiteBroadView, this, configuration);
        this.whiteSdk.joinRoom(roomParams, new AbstractRoomCallbacks() {

            @Override
            public void onPhaseChanged(final RoomPhase phase) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onRoomPhaseChange(phase);
                    }
                });
            }

            @Override
            public void onRoomStateChanged(RoomState modifyState) {
                MemberState memberState = modifyState.getMemberState();
                BroadcastState broadcastState = modifyState.getBroadcastState();
                final SceneState sceneState = modifyState.getSceneState();

                if (memberState != null) {
                    final String applianceName = memberState.getCurrentApplianceName();
                    final int[] sdkColor = memberState.getStrokeColor();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            appliancesTooBar.setState(applianceName, sdkColor);
                        }
                    });
                }
                if (broadcastState != null) {
                    final ViewMode viewMode = broadcastState.getMode();
                    final boolean hasBroadcaster = broadcastState.getBroadcasterInformation() != null;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            broadcastManager.setState(viewMode, hasBroadcaster);
                        }
                    });
                }
                if (sceneState != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            slidesTable.setSceneState(sceneState);
                        }
                    });
                }
            }
        }, new Promise<Room>() {

            @Override
            public void then(final Room room) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setupRoom(room);
                    }
                });
            }

            @Override
            public void catchEx(final SDKError sdkError) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showToast(sdkError.getMessage());
                    }
                });
            }
        });

        this.btnGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickGoBack();
            }
        });
        this.btnReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickReplay();
            }
        });
        this.btnSlides.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSlides();
            }
        });
        this.btnInvite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickInvite();
            }
        });
    }

    private void setupRoom(Room room) {
        this.room = room;
        this.appliancesTooBar.setRoom(room);
        this.broadcastManager.setRoom(room);
        this.slidesTable.setRoom(room);

        if (this.didLeave) {
            room.disconnect();

        } else {
            room.getMemberState(new Promise<MemberState>() {

                @Override
                public void then(MemberState memberState) {
                    String applianceName = memberState.getCurrentApplianceName();
                    int[] sdkColor = memberState.getStrokeColor();
                    appliancesTooBar.setState(applianceName, sdkColor);
                }

                @Override
                public void catchEx(SDKError sdkError) {
                    showToast(sdkError.getMessage());
                }
            });
            room.getBroadcastState(new Promise<BroadcastState>() {

                @Override
                public void then(BroadcastState broadcastState) {
                    final ViewMode viewMode = broadcastState.getMode();
                    final boolean hasBroadcaster = broadcastState.getBroadcasterInformation() != null;
                    broadcastManager.setState(viewMode, hasBroadcaster);
                }

                @Override
                public void catchEx(SDKError sdkError) {
                    showToast(sdkError.getMessage());
                }
            });
            room.getSceneState(new Promise<SceneState>() {

                @Override
                public void then(SceneState sceneState) {
                    slidesTable.setSceneState(sceneState);
                }

                @Override
                public void catchEx(SDKError sdkError) {
                    showToast(sdkError.getMessage());
                }
            });
        }
    }

    private void setButtonsEnable(boolean enable) {
        if (!this.didLeave) {
            if (enable) {
                this.panTopBar.setTranslationY(0);
                this.panBottomBar.setTranslationY(0);
                this.icoLoading.setVisibility(View.INVISIBLE);
            } else {
                int translationDistanceDP = 60;
                float translationDistance = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, translationDistanceDP, this.getResources().getDisplayMetrics()
                );
                this.panTopBar.setTranslationY(- translationDistance);
                this.panBottomBar.setTranslationY(translationDistance);
                this.icoLoading.setVisibility(View.VISIBLE);
            }
            this.appliancesTooBar.setButtonsEnable(enable);
            this.btnInvite.setEnabled(enable);
            this.btnCamera.setEnabled(enable);
            this.btnReplay.setEnabled(enable);
            this.btnSlides.setEnabled(enable);
        }
    }

    private void onClickGoBack() {
        this.finishRoomPage(null);
    }

    private void onClickReplay() {
        Intent resultIntent = new Intent();

        resultIntent.putExtra("uuid", this.uuid);
        resultIntent.putExtra("roomToken", this.roomToken);

        this.finishRoomPage(resultIntent);
    }

    private void finishRoomPage(Intent resultIntent) {
        if (this.room != null) {
            room.disconnect();
        }
        this.didLeave = true;
        this.broadcastManager.dispose();

        if (resultIntent != null) {
            this.setResult(Activity.RESULT_OK, resultIntent);
        }
        this.finish();
    }

    private void onClickSlides() {
        panMain.openDrawer(Gravity.END);
    }

    private void onClickInvite() {
        InviteDialog dialog = new InviteDialog();
        dialog.setUUID(this.uuid);
        dialog.show(getSupportFragmentManager(), "invite dialog");
    }

    private void onRoomPhaseChange(RoomPhase phase) {
        if (phase == RoomPhase.connected) {
            this.showToast("连接成功");
            this.setButtonsEnable(true);

        } else if (phase == RoomPhase.disconnected) {
            this.showToast("断开连接");
            this.setButtonsEnable(false);

        } else if (phase == RoomPhase.reconnecting) {
            this.showToast("重新建立连接");
            this.setButtonsEnable(false);
        }
    }

    private void showToast(Object o) {
        if (!this.didLeave) {
            Toast.makeText(this, o.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
