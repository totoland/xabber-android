package com.xabber.android.presentation.ui.contactlist;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xabber.android.R;
import com.xabber.android.data.account.CommonState;
import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.message.AbstractChat;
import com.xabber.android.data.message.MessageManager;
import com.xabber.android.data.roster.AbstractContact;
import com.xabber.android.presentation.mvp.contactlist.ContactListPresenter;
import com.xabber.android.presentation.mvp.contactlist.ContactListView;
import com.xabber.android.presentation.ui.contactlist.viewobjects.AccountVO;
import com.xabber.android.presentation.ui.contactlist.viewobjects.ChatVO;
import com.xabber.android.ui.activity.ContactListActivity;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.IFlexible;

/**
 * Created by valery.miller on 02.02.18.
 */

public class ContactListFragment extends Fragment implements ContactListView,
        FlexibleAdapter.OnStickyHeaderChangeListener, FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemSwipeListener {

    public static final String ACCOUNT_JID = "account_jid";

    private ContactListPresenter presenter;
    private ContactListFragmentListener contactListFragmentListener;

    private FlexibleAdapter<IFlexible> adapter;
    private List<IFlexible> items;
    private Snackbar snackbar;
    private CoordinatorLayout coordinatorLayout;

    public interface ContactListFragmentListener {
        void onContactClick(AbstractContact contact);
        void onContactListChange(CommonState commonState);
        void onManageAccountsClick();
    }

    public static ContactListFragment newInstance(@Nullable AccountJid account) {
        ContactListFragment fragment = new ContactListFragment();
        Bundle args = new Bundle();
        if (account != null)
            args.putSerializable(ACCOUNT_JID, account);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        contactListFragmentListener = (ContactListFragmentListener) activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        contactListFragmentListener = (ContactListFragmentListener) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        contactListFragmentListener = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list_new, container, false);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        coordinatorLayout = (CoordinatorLayout) view.findViewById(R.id.coordinatorLayout);

        items = new ArrayList<>();
        adapter = new FlexibleAdapter<>(items, null, true);

        adapter.setStickyHeaders(true);
        adapter.setDisplayHeadersAtStartUp(true);
        recyclerView.setAdapter(adapter);

        adapter.setSwipeEnabled(true);
        adapter.expandItemsAtStartUp();
        adapter.addListener(this);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        presenter = ContactListPresenter.getInstance(getActivity());
        ((ContactListActivity)getActivity()).setStatusBarColor();
    }

    @Override
    public void onResume() {
        super.onResume();
        presenter.onLoadContactList(this);
    }

    @Override
    public void updateItems(List<IFlexible> items) {
        this.items.clear();
        this.items.addAll(items);
        adapter.updateDataSet(items, false);
    }

    /**
     * Callback. When new header becomes sticky - change color of status bar.
     */
    @Override
    public void onStickyHeaderChange(int newPosition, int oldPosition) {
        if (newPosition > -1 && adapter.getItemCount() > newPosition) {
            IFlexible item = adapter.getItem(newPosition);
            if (item instanceof AccountVO)
                ((ContactListActivity)getActivity()).setStatusBarColor(((AccountVO)item).getAccountJid()); // account color
            else ((ContactListActivity)getActivity()).setStatusBarColor(); // main color
        }
    }

    @Override
    public boolean onItemClick(int position) {
        adapter.notifyItemChanged(position);
        presenter.onItemClick(adapter.getItem(position));
        return true;
    }

    @Override
    public void onItemSwipe(int position, int direction) {
        Object itemAtPosition = adapter.getItem(position);
        if (itemAtPosition != null && itemAtPosition instanceof ChatVO) {

            // backup of removed item for undo purpose
            final ChatVO deletedItem = (ChatVO) itemAtPosition;

            // update value
            setChatArchived(deletedItem, !(deletedItem).isArchived());

            // remove the item from recycler view
            adapter.removeItem(position);

            // showing snackbar with Undo option
            showSnackbar(deletedItem, position);
        }
    }

    @Override
    public void onActionStateChanged(RecyclerView.ViewHolder viewHolder, int actionState) {

    }

    @Override
    public void onContactClick(AbstractContact contact) {
        contactListFragmentListener.onContactClick(contact);
    }

    public void showSnackbar(final ChatVO deletedItem, final int deletedIndex) {
        if (snackbar != null) snackbar.dismiss();
        final boolean archived = (deletedItem).isArchived();
        snackbar = Snackbar.make(coordinatorLayout, archived ? R.string.chat_was_unarchived
                : R.string.chat_was_archived, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.undo, new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // update value
                setChatArchived((ChatVO) deletedItem, archived);

                // undo is selected, restore the deleted item
                adapter.addItem(deletedIndex, deletedItem);
            }
        });
        snackbar.setActionTextColor(Color.YELLOW);
        snackbar.show();
    }

    public void closeSnackbar() {
        if (snackbar != null) snackbar.dismiss();
    }

    public void setChatArchived(ChatVO chatVO, boolean archived) {
        AbstractChat chat = MessageManager.getInstance().getChat(chatVO.getAccountJid(), chatVO.getUserJid());
        if (chat != null) chat.setArchived(archived, true);
    }

    public void filterContactList(String filter) {
        if (presenter != null) presenter.setFilterString(filter);
    }
}