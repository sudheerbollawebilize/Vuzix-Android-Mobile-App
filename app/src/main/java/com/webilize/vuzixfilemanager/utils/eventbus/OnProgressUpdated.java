package com.webilize.vuzixfilemanager.utils.eventbus;

import com.webilize.vuzixfilemanager.models.TransferModel;

public class OnProgressUpdated {

    public TransferModel transferModel;

    public OnProgressUpdated(TransferModel transferModel) {
        this.transferModel = transferModel;
    }

}
