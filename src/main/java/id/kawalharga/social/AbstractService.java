package id.kawalharga.social;

import id.kawalharga.database.Service;
import id.kawalharga.model.CommodityInput;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by yohanesgultom on 17/06/16.
 */
public abstract class AbstractService {

    protected id.kawalharga.database.Service service;

    public AbstractService(String dbConfig) throws Exception {
        service = id.kawalharga.database.Service.getInstance(dbConfig);
    }

    public Service getService() {
        return service;
    }

    public CommodityInput getInputToBePosted(String socialMediaTable) throws Exception {
        CommodityInput res = null;
        Calendar beginningOfDay = new GregorianCalendar();
        beginningOfDay.set(Calendar.HOUR, 0);
        beginningOfDay.set(Calendar.MINUTE, 0);
        beginningOfDay.set(Calendar.SECOND, 0);
        List<CommodityInput> commodityInputList = this.service.getInputsToBePosted(beginningOfDay.getTime(), 1, socialMediaTable);
        res = (commodityInputList.size() > 0) ? commodityInputList.get(0) : res;
        return res;
    }

    public abstract void postSingleTodayInput() throws Exception;
}
