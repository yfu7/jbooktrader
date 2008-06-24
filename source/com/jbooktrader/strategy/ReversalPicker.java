package com.jbooktrader.strategy;

import com.ib.client.*;
import com.jbooktrader.indicator.balance.*;
import com.jbooktrader.indicator.derivative.*;
import com.jbooktrader.platform.bar.*;
import com.jbooktrader.platform.commission.*;
import com.jbooktrader.platform.indicator.*;
import com.jbooktrader.platform.marketdepth.*;
import com.jbooktrader.platform.model.*;
import com.jbooktrader.platform.optimizer.*;
import com.jbooktrader.platform.schedule.*;
import com.jbooktrader.platform.strategy.*;
import com.jbooktrader.platform.util.*;

/**
 *
 */
public class ReversalPicker extends Strategy {

    // Technical indicators
    private final Indicator scaledBalanceInd, balanceAccelerationInd;

    // Strategy parameters names
    private static final String LOOK_BACK = "LookBack";
    private static final String PERIOD = "Period";
    private static final String ENTRY = "Entry";

    // Strategy parameters values
    private final int entry;


    public ReversalPicker(StrategyParams optimizationParams, MarketBook marketBook, PriceHistory priceHistory) throws JBookTraderException {
        super(optimizationParams, marketBook, priceHistory);

        // Specify the contract to trade
        Contract contract = ContractFactory.makeFutureContract("ES", "GLOBEX");
        int multiplier = 50;// contract multiplier

        // Define trading schedule
        TradingSchedule tradingSchedule = new TradingSchedule("9:20", "16:10", "America/New_York");

        Commission commission = CommissionFactory.getBundledNorthAmericaFutureCommission();
        setStrategy(contract, tradingSchedule, multiplier, commission);

        entry = getParam(ENTRY);

        // Create technical indicators
        scaledBalanceInd = new BalanceEMA(marketBook, getParam(PERIOD));
        balanceAccelerationInd = new Displacement(scaledBalanceInd, getParam(LOOK_BACK));
        addIndicator("BalanceScaled", scaledBalanceInd);
        addIndicator("Acceleration", balanceAccelerationInd);
    }

    /**
     * Adds parameters to strategy. Each parameter must have 5 values:
     * name: identifier
     * min, max, step: range for optimizer
     * value: used in backtesting and trading
     */
    @Override
    public void setParams() {
        addParam(LOOK_BACK, 20, 40, 1, 28);
        addParam(PERIOD, 180, 300, 1, 230);
        addParam(ENTRY, 10, 20, 1, 12);
    }

    /**
     * This method is invoked by the framework when an order book changes and the technical
     * indicators are recalculated. This is where the strategy itself should be defined.
     */
    @Override
    public void onBookChange() {
        double balanceEMA = scaledBalanceInd.getValue();
        double balanceAcceleration = balanceAccelerationInd.getValue();

        if (balanceEMA >= entry && balanceAcceleration < 0) {
            setPosition(1);
        } else if (balanceEMA <= -entry && balanceAcceleration > 0) {
            setPosition(-1);
        }
    }
}
