package com.xabber.android.presentation.mvp.contactlist;

import com.xabber.android.data.roster.AbstractContact;

import java.util.List;

import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public interface ContactListView {

    void updateItems(List<IFlexible> items);
    void onContactClick(AbstractContact contact);
}