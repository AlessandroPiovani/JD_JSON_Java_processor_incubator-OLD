package src;


import ec.satoolkit.seats.SeatsSpecification;
import ec.satoolkit.tramoseats.TramoSeatsSpecification;
import ec.tstoolkit.Parameter;
import ec.tstoolkit.ParameterType;
import ec.tstoolkit.algorithm.ProcessingContext;
import ec.tstoolkit.modelling.DefaultTransformationType;
import ec.tstoolkit.modelling.arima.tramo.ArimaSpec;
import ec.tstoolkit.modelling.arima.tramo.AutoModelSpec;
import ec.tstoolkit.modelling.arima.tramo.EasterSpec;
import ec.tstoolkit.modelling.arima.tramo.EstimateSpec;
import ec.tstoolkit.modelling.arima.tramo.OutlierSpec;
import ec.tstoolkit.modelling.arima.tramo.TradingDaysSpec;
import ec.tstoolkit.modelling.arima.tramo.TransformSpec;
import ec.tstoolkit.timeseries.Day;
import ec.tstoolkit.timeseries.TsPeriodSelector;
import ec.tstoolkit.timeseries.calendars.TradingDaysType;
import ec.tstoolkit.timeseries.regression.OutlierDefinition;
import ec.tstoolkit.timeseries.regression.OutlierType;
import ec.tstoolkit.timeseries.simplets.TsFrequency;
import ec.tstoolkit.timeseries.simplets.TsPeriod;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import static java.util.Objects.isNull;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author cazora
 */
public class TSmodelSetup {

    private final DestSpecificationsModel model;
    private final TramoSeatsSpecification tsSpec;
    private final ProcessingContext context; // Alessandro

    public ProcessingContext getContext() {
        return context;
    }

    public DestSpecificationsModel getModel() {
        return model;
    }

    public TramoSeatsSpecification getTsSpec() {
        return tsSpec;
    }
    
    public TSmodelSetup(DestSpecificationsModel model){
        this.model=model;
        if (model!=null && model.getSpec()!=null) {
            String jsonString =  model.getSpec();
            tsSpec=TramoSeatsSpecification.fromString(model.getSpec());
            setupTSmodel();
        } else {
            tsSpec=TramoSeatsSpecification.RSAfull;
        }
        
        this.context = new ProcessingContext();
    }

    private void setupTSmodel() {
        setTransform();
        setEstimate();
        setTradingDays();
        setEaster();
        setOutliers();
        setAutoModeling();
        setArima();
        setSeats();
    }

    private void setTransform() {
        TransformSpec tf = tsSpec.getTramoSpecification().getTransform();
        if (tf == null) {
            tf = new TransformSpec();
            tsSpec.getTramoSpecification().setTransform(tf);
        }
        tf.setFunction(DefaultTransformationType.valueOf(model.getTransformFunction()));
        tf.setFct(model.getTransformFct());
        tf.setPreliminaryCheck(model.isPreliminaryCheck());
    }

    private void setEstimate() {
        EstimateSpec espec = tsSpec.getTramoSpecification().getEstimate();
        if (espec == null) {
            espec = new EstimateSpec();
            tsSpec.getTramoSpecification().setEstimate(espec);
        }
        /*
            "estimate.urfinal":0.96,
        */
        espec.setUbp(model.getEstimateUrfinal()); // Alessandro
        
        
        espec.setTol(model.getEstimateTol());
        espec.setEML(model.isEstimateEml());
        try {
            if (model.getEstimateFrom()!=null && model.getEstimateTo()!=null) {
                TsPeriodSelector period = new TsPeriodSelector();
                Day from = Day.fromString(model.getEstimateFrom());
                Day to = Day.fromString(model.getEstimateTo());
                period.between(from, to);
                period.excluding(model.getEstimateExclFirst(), model.getEstimateExclLast());
                period.first(model.getEstimateFirst());
                period.last(model.getEstimateLast());
                espec.setSpan(period);
            }
        } catch (Exception e) {
        }
    }

    private void setTradingDays() {
        TradingDaysSpec tdspec = tsSpec.getTramoSpecification().getRegression().getCalendar().getTradingDays();
        if (tdspec == null) {
            tdspec = new TradingDaysSpec();
            tsSpec.getTramoSpecification().getRegression().getCalendar().setTradingDays(tdspec);
        }
        /*
    "tradingdays.option":"None",
         */
        //Alessandro
        if(!isNull(model.getTradingdaysOption()))
        {
            if(model.getTradingdaysOption().equals("TradingDays"))
            {
                tdspec.setTradingDaysType(TradingDaysType.TradingDays);
            }else if(model.getTradingdaysOption().equals("WorkingDays"))
            {
                tdspec.setTradingDaysType(TradingDaysType.WorkingDays);
            }else if(model.getTradingdaysOption().equals("UserDefined"))
            {
            
            }else{
                if(!model.getTradingdaysOption().equals("None") && !model.getTradingdaysOption().equals("NA"))
                {
                    System.out.println("tradingdays.option field has an unknown value");
                    // Stop the program and throw Exception?
                }
                else
                {
                    tdspec.setTradingDaysType(TradingDaysType.None);
                }
        }  
        } 
        // end Alessandro's part
        
     
        tdspec.setAutomaticMethod(TradingDaysSpec.AutoMethod.valueOf(model.getTradingdaysMauto()));
        tdspec.setProbabibilityForFTest(model.getTradingdaysPftd());
        tdspec.setLeapYear(model.isTradingdaysLeapyear());
        tdspec.setStockTradingDays(model.getTradingdaysStocktd());
        tdspec.setTest(model.isTradingdaysTest());
    }

    private void setEaster() {
        EasterSpec ieast = tsSpec.getTramoSpecification().getRegression().getCalendar().getEaster();
        if (ieast == null) {
            ieast = new EasterSpec();
            tsSpec.getTramoSpecification().getRegression().getCalendar().setEaster(ieast);
        }
        ieast.setOption(EasterSpec.Type.valueOf(model.getEasterType()));
        ieast.setDuration(model.getEasterDuration());
        ieast.setJulian(model.isEasterJulian());
        ieast.setTest(model.isEasterTest());

    }

    private void setOutliers() {
    
    // Alessandro
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date;
        String type;
        
        if(!(model.getUsrdefOutliersDate().size() == model.getUsrdefOutliersType().size()))
        {
            System.out.println("Number of userDefined outliers dates is different from the number of outlierTypes");
            // throw exceprtion (?)
        }
        if(!Objects.isNull(model.getUsrdefOutliersDate()) && !(model.getUsrdefOutliersDate().size()==0) && !model.getUsrdefOutliersDate().get(0).equals("NA"))
        {    
            for(int i=0; i<model.getUsrdefOutliersDate().size(); i++)
            {
                try {
                    date = dateFormat.parse(model.getUsrdefOutliersDate().get(i));
                    TsPeriod outlierPeriod = new TsPeriod(TsFrequency.valueOf(model.getFrequency()), date);

                    type = model.getUsrdefOutliersType().get(i);


                    tsSpec.getTramoSpecification().getRegression().add(new 
                                                 OutlierDefinition(outlierPeriod, type));

                } catch (ParseException ex) {
                    Logger.getLogger(TSmodelSetup.class.getName()).log(Level.SEVERE, "Error in user defined outliers", ex);
                }
            }    
        }
    // end Alessandro's part
        
        
        if (model.isOutlierEnabled()) {
            OutlierSpec o = tsSpec.getTramoSpecification().getOutliers();
            if (o == null) {
                o = new OutlierSpec();
                tsSpec.getTramoSpecification().setOutliers(o);
            }
            /*
    "outlier.tcrate":0.7
    "outlier.usedefcv":true/false
             */
            //Alessandro's block
            o.setDeltaTC(model.getOutlierTcrate()); //Alessandro
            if(model.isOutlierUsedefcv())
            {
                o.setCriticalValue(model.getOutlierCv());
            } else{
                o.setCriticalValue(3.5);
            }   
            // end Alessandro's block
            
            if (model.isOutlierAo()) {
                o.add(OutlierType.AO);
            }
            if (model.isOutlierTc()) {
                o.add(OutlierType.TC);
            }
            if (model.isOutlierLs()) {
                o.add(OutlierType.LS);
            }
            if (model.isOutlierSo()) {
                o.add(OutlierType.SO);
            }
            o.setEML(model.isOutlierEml());
            //o.setCriticalValue(model.getOutlierCv()); //placed before into if that controls if it should be read
            
            try {
                if (model.getOutlierFrom()!=null && model.getOutlierTo()!=null) {
                    TsPeriodSelector period = new TsPeriodSelector();
                    Day from = Day.fromString(model.getOutlierFrom());
                    Day to = Day.fromString(model.getOutlierTo());
                    period.between(from, to);
                    period.excluding(model.getOutlierExclFirst(), model.getOutlierExclLast());
                    period.first(model.getOutlierFirst());
                    period.last(model.getOutlierLast());
                    o.setSpan(period);
                }
            } catch (Exception e) {
            }
        }
    }

    private void setAutoModeling() {
        AutoModelSpec aspec = tsSpec.getTramoSpecification().getAutoModel();
        if (aspec == null) {
            aspec = new AutoModelSpec();
            tsSpec.getTramoSpecification().setAutoModel(aspec);
        }
        /*
    "automdl.armalimit":1, 
    "automdl.reducecv":0.12, 
    "automdl.ljungboxlimit":0.95, 

         */
        
        aspec.setTsig(model.getAutomdlArmalimit()); //Alessandro
        aspec.setPc(model.getAutomdlReducecv()); //Alessandro
        aspec.setPcr(model.getAutomdlLjungboxlimit()); //Alessandro

        
        
        aspec.setEnabled(model.isAutomdlEnabled());
        aspec.setAcceptDefault(model.isAutomdlAcceptdefault());
        aspec.setCancel(model.getAutomdlCancel());
        aspec.setUb1(model.getAutomdlUb1());
        aspec.setUb2(model.getAutomdlUb2());
        aspec.setAmiCompare(model.isAutomdlCompare());

    }

    private void setArima() {
        ArimaSpec aspec = tsSpec.getTramoSpecification().getArima();
        if (aspec == null) {
            aspec = new ArimaSpec();
            tsSpec.getTramoSpecification().setArima(aspec);
        }
        /*
    "arima.coefEnabled":true/false, 
    "arima.coef":"NA" o vettore di coefficienti
    "arima.coefType":"NA", o vettore di procedure di stima
         */
        
        //begin Alessandro
        List<String> arimaCoefs     = model.getArimaCoef();
        List<String> arimaCoefTypes = model.getArimaCoefType();
        int p  = aspec.getP();
        int q  = aspec.getQ();
        int bp = aspec.getBP();
        int bq = aspec.getBQ();
        
        if(model.isArimaCoefEnabled() && arimaCoefs!=null && arimaCoefTypes!=null && (arimaCoefs.size() == arimaCoefTypes.size()))
        {   
            if(p>0)
            {
                Parameter[] phiCoefficients= new Parameter[p];
                for(int i=0; i<p; i++)
                {
                    if(arimaCoefTypes.get(i).equals("Undefined"))
                    {
                        phiCoefficients[i]=new Parameter();
                        phiCoefficients[i].setType(ParameterType.Estimated);
                        // need to set soe value?
                    } else if(arimaCoefTypes.get(i).equals("Fixed"))
                    {
                        phiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Fixed);
                    } else if(arimaCoefTypes.get(i).equals("Initial"))
                    {
                        phiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Initial);
                    } else if(arimaCoefTypes.get(i).equals("Derived")) // not present in RJDemetra
                    {
                        phiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Derived);
                    }   
                } 
                aspec.setPhi(phiCoefficients);
            }
            if(q>0)
            {
                    Parameter[] thetaCoefficients= new Parameter[q];
                    for(int i=p; i<q; i++)
                    {
                        if(arimaCoefTypes.get(i).equals("Undefined"))
                        {
                            thetaCoefficients[i]=new Parameter();
                            thetaCoefficients[i].setType(ParameterType.Estimated);
                            // need to set soe value?
                        } else if(arimaCoefTypes.get(i).equals("Fixed"))
                        {
                            thetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Fixed);
                        } else if(arimaCoefTypes.get(i).equals("Initial"))
                        {
                            thetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Initial);
                        } else if(arimaCoefTypes.get(i).equals("Derived")) // not present in RJDemetra
                        {
                            thetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Derived);
                        }   
                    } 
                    aspec.setTheta(thetaCoefficients);      
            }  
            if(bp>0)
            {
                Parameter[] bPhiCoefficients= new Parameter[bp];
                for(int i=q; i<bp; i++)
                {
                    if(arimaCoefTypes.get(i).equals("Undefined"))
                    {
                        bPhiCoefficients[i]=new Parameter();
                        bPhiCoefficients[i].setType(ParameterType.Estimated);
                        // need to set soe value?
                    } else if(arimaCoefTypes.get(i).equals("Fixed"))
                    {
                        bPhiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Fixed);
                    } else if(arimaCoefTypes.get(i).equals("Initial"))
                    {
                        bPhiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Initial);
                    } else if(arimaCoefTypes.get(i).equals("Derived")) // not present in RJDemetra
                    {
                        bPhiCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Derived);
                    }   
                } 
                aspec.setBPhi(bPhiCoefficients);
            }
            if(bq>0)
            {
                    Parameter[] bThetaCoefficients= new Parameter[bq];
                    for(int i=bp; i<bq; i++)
                    {
                        if(arimaCoefTypes.get(i).equals("Undefined"))
                        {
                            bThetaCoefficients[i]=new Parameter();
                            bThetaCoefficients[i].setType(ParameterType.Estimated);
                            // need to set soe value?
                        } else if(arimaCoefTypes.get(i).equals("Fixed"))
                        {
                            bThetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Fixed);
                        } else if(arimaCoefTypes.get(i).equals("Initial"))
                        {
                            bThetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Initial);
                        } else if(arimaCoefTypes.get(i).equals("Derived")) // not present in RJDemetra
                        {
                            bThetaCoefficients[i]=new Parameter(Double.parseDouble(arimaCoefs.get(i)), ParameterType.Derived);
                        }   
                    } 
                    aspec.setBTheta(bThetaCoefficients);         
            }     
        }else if(arimaCoefTypes.size() != arimaCoefs.size())  
        {
         System.out.println("coefficient types and values arrays have not the same length");
         // exception?
        }    
        // end Alessandro's block
        
        aspec.setMean(model.isArimaMu());
        aspec.setP(model.getArimaP());
        aspec.setD(model.getArimaD());
        aspec.setQ(model.getArimaQ());
        aspec.setBD(model.getArimaBD());
        aspec.setBP(model.getArimaBP());
        aspec.setBD(model.getArimaBD());
        aspec.setBQ(model.getArimaBQ());
    }
    

    private void setSeats() {
        SeatsSpecification sspec = tsSpec.getSeatsSpecification();
        if (sspec == null) {
            sspec = new SeatsSpecification();
            tsSpec.setSeatsSpecification(sspec);
        }
        /*"seats.maBoundary":0.95,*/
        sspec.setXlBoundary(model.getSeatsMaBoundary()); //Alessandro
        
        sspec.setPredictionLength(model.getSeatsPredictionLength());
        sspec.setApproximationMode(SeatsSpecification.ApproximationMode.valueOf(model.getSeatsApprox()));
        sspec.setTrendBoundary(model.getSeatsTrendBoundary());
        sspec.setSeasBoundary(model.getSeatsSeasdBoundary());
        sspec.setSeasBoundary1(model.getSeatsSeasdBoundary1());
        sspec.setSeasTolerance(model.getSeatsSeasTol());
        sspec.setMethod(SeatsSpecification.EstimationMethod.valueOf(model.getSeatsMethod()));
    }
}
