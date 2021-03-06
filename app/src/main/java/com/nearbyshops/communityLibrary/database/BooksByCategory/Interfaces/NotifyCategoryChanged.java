package com.nearbyshops.communityLibrary.database.BooksByCategory.Interfaces;

import com.nearbyshops.communityLibrary.database.Model.BookCategory;

/**
 * Created by sumeet on 4/7/16.
 */

public interface NotifyCategoryChanged {

    void categoryChanged(BookCategory currentCategory, boolean isBackPressed);

    void notifySwipeToRight();
}
