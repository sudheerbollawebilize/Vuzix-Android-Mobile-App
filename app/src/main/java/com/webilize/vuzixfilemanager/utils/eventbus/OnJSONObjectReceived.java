package com.webilize.vuzixfilemanager.utils.eventbus;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OnJSONObjectReceived {

    public JSONObject jsonObject;

    public OnJSONObjectReceived(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

}
