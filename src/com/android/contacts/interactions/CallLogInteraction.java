/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.interactions;

import com.android.contacts.R;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.util.BitmapUtil;
import com.android.contacts.common.util.ContactDisplayUtils;
import com.android.contacts.incall.InCallPluginUtils;
import com.android.contacts.quickcontact.QuickContactActivity;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.telephony.PhoneNumberUtils;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;

import com.android.contacts.incall.InCallPluginUtils;
import com.cyanogen.ambient.incall.CallLogConstants;
/**
 * Represents a call log event interaction, wrapping the columns in
 * {@link android.provider.CallLog.Calls}.
 *
 * This class does not return log entries related to voicemail or SIP calls. Additionally,
 * this class ignores number presentation. Number presentation affects how to identify phone
 * numbers. Since, we already know the identity of the phone number owner we can ignore number
 * presentation.
 *
 * As a result of ignoring voicemail and number presentation, we don't need to worry about API
 * version.
 */
public class CallLogInteraction implements ContactInteraction {

    private static final String URI_TARGET_PREFIX = "tel:";
    private static final int CALL_LOG_ICON_RES = R.drawable.ic_phone_24dp;
    private static final int CALL_ARROW_ICON_RES = R.drawable.ic_call_arrow;
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();

    private ContentValues mValues;
    private Drawable mIcon;
    private int mIconResourceId = 0;
    private String mPluginName;

    public CallLogInteraction(ContentValues values) {
        mValues = values;
    }

    @Override
    public Intent getIntent() {
        String number = getNumber();
        Intent intent;
        if (TextUtils.isEmpty(getPluginPkgName())) {
            // regular PSTN
            intent = CallUtil.getCallIntent(getNumber());
        } else {
            // plugin
            intent = new Intent();
            intent.putExtra(InCallPluginUtils.KEY_DATA_ID,
                    QuickContactActivity.CARD_ENTRY_ID_INCALL_PLUGIN_CALL);
            intent.putExtra(InCallPluginUtils.KEY_COMPONENT, getPluginPkgName());
            intent.putExtra(InCallPluginUtils.KEY_NAME, getPluginName());
            intent.putExtra(InCallPluginUtils.KEY_NUMBER, number);
            intent.putExtra(InCallPluginUtils.KEY_MIMETYPE, PhoneNumberUtils.isGlobalPhoneNumber
                    (number) ? ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE : "");
        }
        return intent;
    }

    @Override
    public String getViewHeader(Context context) {
        return getNumber();
    }

    @Override
    public long getInteractionDate() {
        Long date = getDate();
        return date == null ? -1 : date;
    }

    @Override
    public String getViewBody(Context context) {
        Integer numberType = getCachedNumberType();
        if (numberType == null) {
            numberType = Phone.TYPE_CUSTOM;
        }

        final String cachedNumberLabel = getCachedNumberLabel();
        final String label = ContactDisplayUtils.getLabelForCall(context, getNumber(), numberType,
                cachedNumberLabel, mPluginName);

        return label;
    }

    @Override
    public String getViewFooter(Context context) {
        Long date = getDate();
        return date == null ? null : ContactInteractionUtil.formatDateStringFromTimestamp(
                date, context);
    }

    @Override
    public Drawable getIcon(Context context) {
        if (mIcon != null) {
            // it's a plugin interaction
            return mIcon;
        } else {
            return context.getResources().getDrawable(CALL_LOG_ICON_RES);
        }
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        Drawable callArrow = null;
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return null;
        }
        switch (type) {
            case Calls.INCOMING_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.MISSED_TYPE:
                callArrow = res.getDrawable(CALL_ARROW_ICON_RES);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_red),
                        PorterDuff.Mode.MULTIPLY);
                break;
            case Calls.OUTGOING_TYPE:
                callArrow = BitmapUtil.getRotatedDrawable(res, CALL_ARROW_ICON_RES, 180f);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green),
                        PorterDuff.Mode.MULTIPLY);
                break;
        }
        return callArrow;
    }

    public String getCachedName() {
        return mValues.getAsString(Calls.CACHED_NAME);
    }

    public String getCachedNumberLabel() {
        return mValues.getAsString(Calls.CACHED_NUMBER_LABEL);
    }

    public Integer getCachedNumberType() {
        return mValues.getAsInteger(Calls.CACHED_NUMBER_TYPE);
    }

    public Long getDate() {
        return mValues.getAsLong(Calls.DATE);
    }

    public Long getDuration() {
        return mValues.getAsLong(Calls.DURATION);
    }

    public Boolean getIsRead() {
        return mValues.getAsBoolean(Calls.IS_READ);
    }

    public Integer getLimitParamKey() {
        return mValues.getAsInteger(Calls.LIMIT_PARAM_KEY);
    }

    public Boolean getNew() {
        return mValues.getAsBoolean(Calls.NEW);
    }

    public String getNumber() {
        final String number = mValues.getAsString(Calls.NUMBER);
        return number == null ? null :
            sBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR);
    }

    public Integer getNumberPresentation() {
        return mValues.getAsInteger(Calls.NUMBER_PRESENTATION);
    }

    public Integer getOffsetParamKey() {
        return mValues.getAsInteger(Calls.OFFSET_PARAM_KEY);
    }

    public Integer getType() {
        return mValues.getAsInteger(Calls.TYPE);
    }

    @Override
    public Spannable getContentDescription(Context context) {
        final String phoneNumber = getViewHeader(context);
        final String contentDescription = context.getResources().getString(
                R.string.content_description_recent_call,
                getCallTypeString(context), phoneNumber, getViewFooter(context));
        return ContactDisplayUtils.getTelephoneTtsSpannable(contentDescription, phoneNumber);
    }

    private String getCallTypeString(Context context) {
        String callType = "";
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return callType;
        }
        switch (type) {
            case Calls.INCOMING_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_incoming);
                break;
            case Calls.MISSED_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_missed);
                break;
            case Calls.OUTGOING_TYPE:
                callType = res.getString(R.string.content_description_recent_call_type_outgoing);
                break;
        }
        return callType;
    }

    @Override
    public int getIconResourceId() {
        if (mIconResourceId != 0) {
            return mIconResourceId;
        } else {
            return CALL_LOG_ICON_RES;
        }
    }

    public void setPluginInfo(Context context, int resourceId, String pluginName) {
        ComponentName compName = ComponentName.unflattenFromString(getPluginPkgName());
        mIcon = InCallPluginUtils.getDrawable(context, resourceId, compName);
        mIconResourceId = resourceId;
        mPluginName = pluginName;
    }

    public String getPluginPkgName() {
        return mValues.getAsString(CallLogConstants.PLUGIN_PACKAGE_NAME);
    }

    public String getPluginUserHandle() {
        return mValues.getAsString(CallLogConstants.PLUGIN_USER_HANDLE);
    }

    public String getPluginName() {
        return mPluginName;
    }
}
