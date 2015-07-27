/*
 * Copyright 2011 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.guvnor.inbox.client.editor;

import java.util.*;

import com.github.gwtbootstrap.client.ui.Button;
import com.github.gwtbootstrap.client.ui.constants.IconType;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.DateCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import org.guvnor.inbox.client.InboxPresenter;
import org.guvnor.inbox.client.resources.i18n.InboxConstants;
import org.guvnor.inbox.client.resources.images.ImageResources;
import org.guvnor.inbox.model.InboxPageRequest;
import org.guvnor.inbox.model.InboxPageRow;
import org.guvnor.inbox.service.InboxService;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.uberfire.ext.widgets.common.client.tables.ComparableImageResource;
import org.uberfire.ext.widgets.common.client.tables.ComparableImageResourceCell;
import org.uberfire.ext.widgets.common.client.tables.PagedTable;
import org.uberfire.paging.PageResponse;

/**
 * Widget with a table of inbox entries results.
 */
public class InboxViewImpl extends PagedTable<InboxPageRow> implements InboxView {

    private static final int PAGE_SIZE = 10;

    public InboxViewImpl( final Caller<InboxService> inboxService,
                          final String inboxName,
                          final InboxPresenter presenter ) {
        super( PAGE_SIZE );
        Column<InboxPageRow, String> openColumn = new Column<InboxPageRow, String>( new ButtonCell() ) {
            public String getValue( final InboxPageRow row ) {
                return InboxConstants.INSTANCE.open();
            }
        };

        openColumn.setFieldUpdater( new FieldUpdater<InboxPageRow, String>() {
            public void update( final int index,
                                final InboxPageRow row,
                                final String value ) {
                presenter.open( row );
            }
        } );

        addColumn( openColumn,
                   InboxConstants.INSTANCE.open() );

        Column<InboxPageRow, ComparableImageResource> formatColumn = new Column<InboxPageRow, ComparableImageResource>( new ComparableImageResourceCell() ) {

            public ComparableImageResource getValue( InboxPageRow row ) {
                return new ComparableImageResource( row.getFormat(),
                                                    new Image( ImageResources.INSTANCE.fileIcon() ) );
            }
        };
        addColumn( formatColumn,
                   InboxConstants.INSTANCE.format() );

        TextColumn<InboxPageRow> noteColumn = new TextColumn<InboxPageRow>() {
            public String getValue( InboxPageRow row ) {
                return row.getNote();
            }
        };
        noteColumn.setSortable( true );
        noteColumn.setDataStoreName( "name" );
        addColumn( noteColumn,
                   InboxConstants.INSTANCE.name() );

        Column<InboxPageRow, Date> dateColumn = new Column<InboxPageRow, Date>( new DateCell( DateTimeFormat.getFormat( DateTimeFormat.PredefinedFormat.DATE_TIME_MEDIUM ) ) ) {
            public Date getValue( InboxPageRow row ) {
                return row.getTimestamp();
            }
        };
        dateColumn.setSortable( true );
        dateColumn.setDataStoreName( "timestamp" );
        addColumn( dateColumn,
                   InboxConstants.INSTANCE.createdDate() );

        final AsyncDataProvider<InboxPageRow> dataProvider = new AsyncDataProvider<InboxPageRow>() {
            protected void onRangeChanged( HasData<InboxPageRow> display ) {
                ColumnSortList columnSortList = dataGrid.getColumnSortList();

                final InboxPageRequest request = new InboxPageRequest();
                request.setInboxName( inboxName );
                request.setStartRowIndex( dataGrid.getPageStart() );
                request.setPageSize( PAGE_SIZE );

                if ( columnSortList.size() > 0 ) {
                    request.setSortColumnName( columnSortList.get( 0 ).getColumn().getDataStoreName() );
                    request.setSortAscending( columnSortList.get( 0 ).isAscending() );
                } else {
                    request.setSortColumnName( "" );
                    request.setSortAscending( true );
                }

                inboxService.call( new RemoteCallback<PageResponse<InboxPageRow>>() {
                    @Override
                    public void callback( final PageResponse<InboxPageRow> response ) {
                        if ( request.getSortColumnName().equals( "name" ) ) {
                            Collections.sort( response.getPageRowList(), new Comparator<InboxPageRow>() {
                                public int compare( InboxPageRow o1, InboxPageRow o2 ) {
                                    if ( o1 == o2 ) {
                                        return 0;
                                    }

                                    // Compare the name columns.
                                    int diff = -1;
                                    if ( o1 != null ) {
                                        diff = ( o2 != null ) ? o1.getNote().compareTo( o2.getNote() ) : 1;
                                    }
                                    return request.isSortAscending() ? diff : -diff;
                                }
                            } );
                        } else if ( request.getSortColumnName().equals( "timestamp" ) ) {
                            Collections.sort( response.getPageRowList(), new Comparator<InboxPageRow>() {
                                public int compare( InboxPageRow o1, InboxPageRow o2 ) {
                                    if ( o1 == o2 ) {
                                        return 0;
                                    }

                                    // Compare the name columns.
                                    int diff = -1;
                                    if ( o1 != null ) {
                                        diff = ( o2 != null ) ? o1.getTimestamp().compareTo( o2.getTimestamp() ) : 1;
                                    }
                                    return request.isSortAscending() ? diff : -diff;
                                }
                            } );
                        }

                        updateRowCount( response.getTotalRowSize(),
                                response.isTotalRowSizeExact() );
                        updateRowData( response.getStartRowIndex(),
                                response.getPageRowList() );
                    }
                } ).loadInbox( request );


            }
        };
        ColumnSortEvent.AsyncHandler columnSortHandler = new ColumnSortEvent.AsyncHandler(dataGrid) {

            @Override
            public void onColumnSort(final ColumnSortEvent event) {
                if(dataProvider.getDataDisplays().size()>0) {
                    HasData<InboxPageRow> next = dataProvider.getDataDisplays().iterator().next();
                    dataProvider.getDataDisplays().iterator().next().setVisibleRangeAndClearData( next.getVisibleRange(), true );
                }
            }
        };
        addColumnSortHandler(columnSortHandler);
        setDataProvider( dataProvider );


        final Button refreshButton = new Button();
        refreshButton.setIcon( IconType.REFRESH );
        refreshButton.setTitle( InboxConstants.INSTANCE.refresh() );
        refreshButton.addClickHandler( new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                refresh();
            }
        } );
        getToolbar().add( refreshButton );
    }

}
