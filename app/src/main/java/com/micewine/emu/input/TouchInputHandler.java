// Copyright 2013 The Chromium Authors
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.micewine.emu.input;

import static com.micewine.emu.activities.EmulationActivity.getDisplayDensity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Build;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.IntDef;
import androidx.core.math.MathUtils;

import com.micewine.emu.LorieView;
import com.micewine.emu.activities.EmulationActivity;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

/**
 * This class is responsible for handling Touch input from the user.  Touch events which manipulate
 * the local canvas are handled in this class and any input which should be sent to the remote host
 * are passed to the InputStrategyInterface implementation set by the DesktopView.
 */
public class TouchInputHandler {
    private static final float EPSILON = 0.001f;

    /** Used to set/store the selected input mode. */
    @SuppressWarnings("unused")
    @IntDef({InputMode.TRACKPAD, InputMode.SIMULATED_TOUCH, InputMode.TOUCH})
    @Retention(RetentionPolicy.SOURCE)
    public @interface InputMode {
        // Values are starting from 0 and don't have gaps.
        int TRACKPAD = 1;
        int SIMULATED_TOUCH = 2;
        int TOUCH = 3;
    }

    private final RenderData mRenderData;
    private final GestureDetector mScroller;
    private final TapGestureDetector mTapDetector;
    private final HardwareMouseListener mHMListener = new HardwareMouseListener();
    private final TouchInputHandler mTouchpadHandler;

    /** Used to disambiguate a 2-finger gesture as a swipe or a pinch. */
    private final SwipeDetector mSwipePinchDetector;

    private InputStrategyInterface mInputStrategy;
    private final InputEventSender mInjector;
    private final EmulationActivity mActivity;
    private float mDensity;

    private final BiConsumer<Integer, Boolean> noAction = (key, down) -> {};
    private final BiConsumer<Integer, Boolean> swipeUpAction = noAction;
    private final BiConsumer<Integer, Boolean> swipeDownAction = noAction;
    /**
     * Used for tracking swipe gestures. Only the Y-direction is needed for responding to swipe-up
     * or swipe-down.
     */
    private float mTotalMotionY;

    /**
     * Distance in pixels beyond which a motion gesture is considered to be a swipe. This is
     * initialized using the Context passed into the constructor.
     */
    private final float mSwipeThreshold;

    /**
     * Set to true to prevent any further movement of the cursor, for example, when showing the
     * keyboard to prevent the cursor wandering from the area where keystrokes should be sent.
     */
    private boolean mSuppressCursorMovement;

    /**
     * Set to true when 3-finger swipe gesture is complete, so that further movement doesn't
     * trigger more swipe actions.
     */
    private boolean mSwipeCompleted;

    /**
     * Set to true when a 1 finger pan gesture originates with a long-press.  This means the user
     * is performing a drag operation.
     */
    private boolean mIsDragging;

    private final int[][] buttons = {
            {MotionEvent.BUTTON_PRIMARY, InputStub.BUTTON_LEFT},
            {MotionEvent.BUTTON_TERTIARY, InputStub.BUTTON_MIDDLE},
            {MotionEvent.BUTTON_SECONDARY, InputStub.BUTTON_RIGHT}
    };
    private int savedBS = 0;
    private int currentBS = 0;
    boolean isMouseButtonChanged(int mask) {
        return (savedBS & mask) != (currentBS & mask);
    }

    boolean mouseButtonDown(int mask) {
        return ((currentBS & mask) != 0);
    }

    private TouchInputHandler(EmulationActivity activity, RenderData renderData,
                              final InputEventSender injector, boolean isTouchpad) {
        if (injector == null)
            throw new NullPointerException();

        mRenderData = renderData != null ? renderData :new RenderData();
        mInjector = injector;
        mActivity = activity;

        GestureListener listener = new GestureListener();
        mScroller = new GestureDetector(/*desktop*/ activity, listener, null, false);

        // If long-press is enabled, the gesture-detector will not emit any further onScroll
        // notifications after the onLongPress notification. Since onScroll is being used for
        // moving the cursor, it means that the cursor would become stuck if the finger were held
        // down too long.
        mScroller.setIsLongpressEnabled(false);

        mTapDetector = new TapGestureDetector(/*desktop*/ activity, listener);
        mSwipePinchDetector = new SwipeDetector(/*desktop*/ activity);

        // The threshold needs to be bigger than the ScaledTouchSlop used by the gesture-detectors,
        // so that a gesture cannot be both a tap and a swipe. It also needs to be small enough so
        // that intentional swipes are usually detected.
        float density = /*desktop*/ activity.getResources().getDisplayMetrics().density;
        mSwipeThreshold = 40 * density;

        setInputMode(InputMode.TRACKPAD);
        mTouchpadHandler = isTouchpad ? null : new TouchInputHandler(activity, mRenderData, injector, true);

        refreshInputDevices();
        ((InputManager) mActivity.getSystemService(Context.INPUT_SERVICE)).registerInputDeviceListener(new InputManager.InputDeviceListener() {
            @Override
            public void onInputDeviceAdded(int deviceId) {
                InputDevice dev = InputDevice.getDevice(deviceId);
                String name = dev != null ? dev.getName() : "null";
                android.util.Log.d("InputDeviceListener", "added " + name);
                refreshInputDevices();
            }

            @Override
            public void onInputDeviceRemoved(int deviceId) {
                android.util.Log.d("InputDeviceListener", "device removed");
                refreshInputDevices();
            }

            @Override
            public void onInputDeviceChanged(int deviceId) {
                InputDevice dev = InputDevice.getDevice(deviceId);
                String name = dev != null ? dev.getName() : "null";
                android.util.Log.d("InputDeviceListener", "changed " + name);
                refreshInputDevices();
            }
        }, null);

    }

    public TouchInputHandler(EmulationActivity activity, final InputEventSender injector) {
        this(activity, null, injector, false);
    }

    static public void refreshInputDevices() {
        AtomicBoolean stylusAvailable = new AtomicBoolean(false);
        AtomicBoolean externalKeyboardAvailable = new AtomicBoolean(false);
        android.util.Log.d("DEVICES", "external keyboard connected " + stylusAvailable.get());
        Arrays.stream(InputDevice.getDeviceIds())
                .mapToObj(InputDevice::getDevice)
                .filter(Objects::nonNull)
                .forEach((device) -> {
                    //noinspection DataFlowIssue
                    android.util.Log.d("DEVICES", "found device \"" + device.getName() + "\" " +
                            (device.supportsSource(InputDevice.SOURCE_STYLUS) ? ((isExternal(device) ? "external " : "") + "stylus ") : "") +
                            ((device.supportsSource(InputDevice.SOURCE_KEYBOARD) && device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) ? ((isExternal(device) ? "external " : "") + "keyboard ") : "") +
                            "sources " + String.format("0x%08X", device.getSources()));

                    if (device.supportsSource(InputDevice.SOURCE_STYLUS))
                        stylusAvailable.set(true);

                    if (device.supportsSource(InputDevice.SOURCE_KEYBOARD) && device.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC && isExternal(device))
                        externalKeyboardAvailable.set(true);
                });
        android.util.Log.d("DEVICES", "requesting stylus " + stylusAvailable.get());
        android.util.Log.d("DEVICES", "external keyboard connected " + externalKeyboardAvailable.get());

        LorieView.requestStylusEnabled(stylusAvailable.get());
        EmulationActivity.getInstance().setExternalKeyboardConnected(externalKeyboardAvailable.get());
    }

    public boolean handleTouchEvent(View view0, View view, MotionEvent event) {
        // Regular touchpads (in captured mode) send events as finger too,
        // but they should be handled as touchscreens with trackpad mode.
        if (mTouchpadHandler != null && ((event.getToolType(event.getActionIndex()) == MotionEvent.TOOL_TYPE_FINGER &&
                (event.getSource() & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD)))
            return mTouchpadHandler.handleTouchEvent(view0, view, event);

        if (view0 != view) {
            int[] view0Location = new int[2];
            int[] viewLocation = new int[2];

            view0.getLocationInWindow(view0Location);
            view.getLocationInWindow(viewLocation);

            int offsetX = viewLocation[0] - view0Location[0];
            int offsetY = viewLocation[1] - view0Location[1];

            event.offsetLocation(-offsetX, -offsetY);
        }

        if (!view.isFocused() && event.getAction() == MotionEvent.ACTION_DOWN) {
            view.requestFocus();
        }

        if ((event.getToolType(event.getActionIndex()) == MotionEvent.TOOL_TYPE_MOUSE
                || (event.getSource() & InputDevice.SOURCE_MOUSE) == InputDevice.SOURCE_MOUSE)
                || (event.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE)
            return mHMListener.onTouch(view, event);

        if (event.getToolType(event.getActionIndex()) == MotionEvent.TOOL_TYPE_FINGER) {
            // Give the underlying input strategy a chance to observe the current motion event before
            // passing it to the gesture detectors.  This allows the input strategy to react to the
            // event or save the payload for use in recreating the gesture remotely.
            if (mInputStrategy instanceof InputStrategyInterface.NullInputStrategy)
                mInjector.sendTouchEvent(event, mRenderData);
            else
                mInputStrategy.onMotionEvent(event);

            // Avoid short-circuit logic evaluation - ensure all gesture detectors see all events so
            // that they generate correct notifications.
            mScroller.onTouchEvent(event);
            mTapDetector.onTouchEvent(event);
            mSwipePinchDetector.onTouchEvent(event);

            // For hardware touchpad in DeX (captured mode), handle physical click buttons
            if ((event.getSource() & InputDevice.SOURCE_TOUCHPAD) == InputDevice.SOURCE_TOUCHPAD) {
                currentBS = event.getButtonState();
                for (int[] button: buttons)
                    if (isMouseButtonChanged(button[0]))
                        mInjector.sendMouseEvent(null, button[1], mouseButtonDown(button[0]), true);
                savedBS = currentBS;
            }
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mSuppressCursorMovement = false;
                    mSwipeCompleted = false;
                    mIsDragging = false;
                    break;

                case MotionEvent.ACTION_SCROLL:
                    float scrollY = -100 * event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                    float scrollX = -100 * event.getAxisValue(MotionEvent.AXIS_HSCROLL);

                    mInjector.sendMouseWheelEvent(scrollX, scrollY);
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    mTotalMotionY = 0;
                    break;

                default:
                    break;
            }
            return true;
        }

        return false;
    }

    private void resetTransformation() {
        float sx = (float) mRenderData.screenWidth / (float) mRenderData.imageWidth;
        float sy = (float) mRenderData.screenHeight / (float) mRenderData.imageHeight;
        mRenderData.scale.set(sx, sy);
    }

    public void handleClientSizeChanged(int w, int h) {
        mRenderData.screenWidth = w;
        mRenderData.screenHeight = h;

        if (mTouchpadHandler != null)
            mTouchpadHandler.handleClientSizeChanged(w, h);

        resetTransformation();
    }

    public void handleHostSizeChanged(int w, int h) {
        mRenderData.imageWidth = w;
        mRenderData.imageHeight = h;

        if (mTouchpadHandler != null)
            mTouchpadHandler.handleHostSizeChanged(w, h);

        resetTransformation();
        mDensity = getDisplayDensity();
    }

    public void setInputMode(@InputMode int inputMode) {
        if (mTouchpadHandler == null)
            mInputStrategy = new InputStrategyInterface.TrackpadInputStrategy(mInjector);
        else if (inputMode == InputMode.TOUCH)
            mInputStrategy = new InputStrategyInterface.NullInputStrategy();
        else if (inputMode == InputMode.SIMULATED_TOUCH)
            mInputStrategy = new InputStrategyInterface.SimulatedTouchInputStrategy(mRenderData, mInjector, mActivity);
        else
            mInputStrategy = new InputStrategyInterface.TrackpadInputStrategy(mInjector);
    }

    public static boolean isExternal(InputDevice d) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            return d.isExternal();

        try {
            // isExternal is a hidden method that is not accessible through the SDK_INT before Android Q
            //noinspection DataFlowIssue
            return (Boolean) InputDevice.class.getMethod("isExternal").invoke(d);
        } catch (NullPointerException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return false;
        }
    }

    private void moveCursorByOffset(float deltaX, float deltaY) {
        if ((deltaX > 0.08 || deltaX < -0.08) && (deltaY > 0.08 || deltaY < -0.08)) {
            if (mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy)
                mInjector.sendCursorMove(-deltaX, -deltaY, true);
            else if (mInputStrategy instanceof InputStrategyInterface.SimulatedTouchInputStrategy) {
                PointF cursorPos = mRenderData.getCursorPosition();
                cursorPos.offset(-deltaX, -deltaY);
                cursorPos.set(MathUtils.clamp(cursorPos.x, 0, mRenderData.screenWidth), MathUtils.clamp(cursorPos.y, 0, mRenderData.screenHeight));
                if (mRenderData.setCursorPosition(cursorPos.x, cursorPos.y))
                    mInjector.sendCursorMove((int) cursorPos.x, (int) cursorPos.y, false);
            }
        }
    }

    /** Moves the cursor to the specified position on the screen. */
    private void moveCursorToScreenPoint(float screenX, float screenY) {
        if (mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy || mInputStrategy instanceof InputStrategyInterface.SimulatedTouchInputStrategy) {
            float[] imagePoint = {screenX * mRenderData.scale.x, screenY * mRenderData.scale.y};
            if (mRenderData.setCursorPosition(imagePoint[0], imagePoint[1]))
                mInjector.sendCursorMove((int) imagePoint[0], imagePoint[1], false);
        }
    }

    /** Processes a (multi-finger) swipe gesture. */
    private boolean onSwipe() {
        if (mTotalMotionY > mSwipeThreshold)
            swipeDownAction.accept(0, true);
        else if (mTotalMotionY < -mSwipeThreshold)
            swipeUpAction.accept(0, true);
        else
            return false;

        mSuppressCursorMovement = true;
        mSwipeCompleted = true;
        return true;
    }

    /** Responds to touch events filtered by the gesture detectors.
     * @noinspection NullableProblems */
    private class GestureListener extends GestureDetector.SimpleOnGestureListener
            implements TapGestureDetector.OnTapListener {
        private final Handler mGestureListenerHandler = new Handler(msg -> {
            if (msg.what == InputStub.BUTTON_LEFT)
                mInputStrategy.onTap(InputStub.BUTTON_LEFT);
            return true;
        });

        /**
         * Called when the user drags one or more fingers across the touchscreen.
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            int pointerCount = e2.getPointerCount();

            if (pointerCount >= 3 && !mSwipeCompleted) {
                // Note that distance values are reversed. For example, dragging a finger in the
                // direction of increasing Y coordinate (downwards) results in distanceY being
                // negative.
                mTotalMotionY -= distanceY;
                return onSwipe();
            }

            if (pointerCount == 2 && mSwipePinchDetector.isSwiping()) {
                if (!(mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy)) {
                    // Ensure the cursor is located at the coordinates of the original event,
                    // otherwise the target window may not receive the scroll event correctly.
                    moveCursorToScreenPoint(e1.getX(), e1.getY());
                }
                mInputStrategy.onScroll(distanceX, distanceY);

                // Prevent the cursor being moved or flung by the gesture.
                mSuppressCursorMovement = true;
                return true;
            }

            if (pointerCount != 1 || mSuppressCursorMovement)
                return false;

            if (mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy) {
                if (mInjector.scaleTouchpad) {
                    distanceX *= mRenderData.scale.x;
                    distanceY *= mRenderData.scale.y;
                }
                moveCursorByOffset(distanceX, distanceY);
            }
            if (!(mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy) && mIsDragging) {
                // Ensure the cursor follows the user's finger when the user is dragging under
                // direct input mode.
                moveCursorToScreenPoint(e2.getX(), e2.getY());
            }
            return true;
        }

        /** Called whenever a gesture starts. Always accepts the gesture so it isn't ignored. */
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        /**
         * Called when the user taps the screen with one or more fingers.
         */
        @Override
        public void onTap(int pointerCount, float x, float y) {
            int button = mouseButtonFromPointerCount(pointerCount);
            if (button == InputStub.BUTTON_UNDEFINED)
                return;

            if (!(mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy)) {
                if (screenPointLiesOutsideImageBoundary(x, y))
                    return;

                moveCursorToScreenPoint(x, y);
            }

            if (button != InputStub.BUTTON_LEFT || !(mInjector.tapToMove && mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy))
                mInputStrategy.onTap(button);
            else
                mGestureListenerHandler.sendEmptyMessageDelayed(InputStub.BUTTON_LEFT, ViewConfiguration.getDoubleTapTimeout());
        }

        private float mLastFocusX;
        private float mLastFocusY;
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getPointerCount() == 1) {
                switch(e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        if (mInjector.tapToMove && mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy) {
                            mGestureListenerHandler.removeMessages(InputStub.BUTTON_LEFT);
                            if (mInputStrategy.onPressAndHold(InputStub.BUTTON_LEFT, true))
                                mIsDragging = true;
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        onScroll(null, e, mLastFocusX - e.getX(), mLastFocusY - e.getY());
                        break;
                }

                mLastFocusX = e.getX();
                mLastFocusY = e.getY();
            }

            return true;
        }

        /** Called when a long-press is triggered for one or more fingers. */
        @Override
        public void onLongPress(int pointerCount, float x, float y) {
            int button = mouseButtonFromPointerCount(pointerCount);
            if (button == InputStub.BUTTON_UNDEFINED) {
                return;
            }

            if (!(mInputStrategy instanceof InputStrategyInterface.TrackpadInputStrategy)) {
                if (screenPointLiesOutsideImageBoundary(x, y))
                    return;
                moveCursorToScreenPoint(x, y);
            }

            if (mInputStrategy.onPressAndHold(button, false))
                mIsDragging = true;
        }

        /** Maps the number of fingers in a tap or long-press gesture to a mouse-button. */
        private int mouseButtonFromPointerCount(int pointerCount) {
            return switch (pointerCount) {
                case 1 -> InputStub.BUTTON_LEFT;
                case 2 -> InputStub.BUTTON_RIGHT;
                case 3 -> InputStub.BUTTON_MIDDLE;
                default -> InputStub.BUTTON_UNDEFINED;
            };
        }

        /** Determines whether the given screen point lies outside the desktop image. */
        private boolean screenPointLiesOutsideImageBoundary(float screenX, float screenY) {
            float scaledX = screenX * mRenderData.scale.x, scaledY = screenY * mRenderData.scale.y;

            float imageWidth = (float) mRenderData.imageWidth + EPSILON;
            float imageHeight = (float) mRenderData.imageHeight + EPSILON;

            return scaledX < -EPSILON || scaledX > imageWidth || scaledY < -EPSILON || scaledY > imageHeight;
        }
    }

    public boolean sendKeyEvent(KeyEvent e) {
        return mInjector.sendKeyEvent(e);
    }

    private class HardwareMouseListener {
        private int savedBS = 0;
        private int currentBS = 0;

        boolean isMouseButtonChanged(int mask) {
            return (savedBS & mask) != (currentBS & mask);
        }

        boolean mouseButtonDown(int mask) {
            return ((currentBS & mask) != 0);
        }

        private final int[][] buttons = {
                {MotionEvent.BUTTON_PRIMARY, InputStub.BUTTON_LEFT},
                {MotionEvent.BUTTON_TERTIARY, InputStub.BUTTON_MIDDLE},
                {MotionEvent.BUTTON_SECONDARY, InputStub.BUTTON_RIGHT}
        };

        /** @noinspection ReassignedVariable */
        @SuppressLint("ClickableViewAccessibility")
        boolean onTouch(View v, MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_SCROLL) {
                float scrollY = -100 * e.getAxisValue(MotionEvent.AXIS_VSCROLL);
                float scrollX = -100 * e.getAxisValue(MotionEvent.AXIS_HSCROLL);

                mInjector.sendMouseWheelEvent(scrollX, scrollY);
                return true;
            }

            if (v.hasPointerCapture()) {
                if (e.getAction() == MotionEvent.ACTION_MOVE && e.getPointerCount() == 1) {
                    boolean axis_relative_x = e.getDevice().getMotionRange(MotionEvent.AXIS_RELATIVE_X) != null;
                    boolean mouse_relative = (e.getSource() & InputDevice.SOURCE_MOUSE_RELATIVE) == InputDevice.SOURCE_MOUSE_RELATIVE;
                    if (axis_relative_x || mouse_relative) {
                        float x = axis_relative_x ? e.getAxisValue(MotionEvent.AXIS_RELATIVE_X) : e.getX();
                        float y = axis_relative_x ? e.getAxisValue(MotionEvent.AXIS_RELATIVE_Y) : e.getY();

                        x *= mDensity;
                        y *= mDensity;

                        mInjector.sendCursorMove(x, y, true);
                        if (axis_relative_x && mTouchpadHandler != null)
                            mTouchpadHandler.mTapDetector.onTouchEvent(e);
                    }
                }
            }

            currentBS = e.getButtonState();
            for (int[] button: buttons)
                if (isMouseButtonChanged(button[0]))
                    mInjector.sendMouseEvent(null, button[1], mouseButtonDown(button[0]), true);
            savedBS = currentBS;
            return true;
        }
    }
}
