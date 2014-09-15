package com.example.alexander.assignment1b;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;


public class TipCalculatorActivity extends Activity implements AdapterView.OnItemSelectedListener, RadioGroup.OnCheckedChangeListener, Chronometer.OnChronometerTickListener, SeekBar.OnSeekBarChangeListener, TextWatcher{

    // For user interface components
    Spinner spinnerSkill;
    Spinner spinnerCountry;
    RadioGroup radioGroupAvailability;
    CheckBox checkBoxFriendly;
    CheckBox checkBoxSpecials;
    CheckBox checkBoxRecommends;
    TextView billTv;
    TextView normalTipTv;
    TextView tippingTv;
    TextView toPayTv;
    MenuItem menuItemStart;
    MenuItem menuItemPause;
    MenuItem menuItemReset;
    SeekBar tipSlider;
    Chronometer waiterTimer;




    // Tipping percentage for the service
    private double tipPercentage = 0;
    private int tipPercentageMin;
    private double tipPercentageSpan;

    // Seconds waited before service
    private int secondsWaited = 0;


    // Keeps tracks of service scores
    private int scoreNice = 0;
    private int scoreSpecial = 0;
    private int scoreRecommends = 0;
    private int scoreAvailability = 0;
    private int scoreProblemSolving = 0;
    // Assume good service speed if unmeasured
    private int scoreSpeed = 1;

    // Number of areas that are counted towards score total
    private static final int countScoreAreas = 6;

    private int scoreTotal = 0;


    private static final int INCOMPETENT = 0;
    private static final int MEDIOCRE = 1;
    private static final int COMPETENT = 2;

    private static final int ICELAND = 0;
    private static final int SWITZERLAND = 1;
    private static final int GREECE = 2;
    private static final int USA = 3;

    private static final int SCORE_BAD = -1;
    private static final int SCORE_OK = 0;
    private static final int SCORE_GOOD = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tip_calculator);

        // Find and prepare spinners
        spinnerSkill = findSpinnerAdaptStringArray(R.id.spinnerSkill, R.array.skill_ratings);
        spinnerCountry = findSpinnerAdaptStringArray(R.id.spinnerCountry,R.array.countries);

        // Finds the rest of the views
        radioGroupAvailability = (RadioGroup) findViewById(R.id.radioGroupAvailability);
        checkBoxFriendly = (CheckBox) findViewById(R.id.checkBoxFriendly);
        checkBoxSpecials = (CheckBox) findViewById(R.id.checkBoxSpecials);
        checkBoxRecommends = (CheckBox) findViewById(R.id.checkBoxRecommends);
        tippingTv = (TextView) findViewById(R.id.tipAmount);
        waiterTimer = (Chronometer) findViewById(R.id.chronometer);
        menuItemStart = (MenuItem) findViewById(R.id.action_timer_start);
        menuItemPause = (MenuItem) findViewById(R.id.action_timer_pause);
        menuItemReset = (MenuItem) findViewById(R.id.action_timer_reset);
        tipSlider = (SeekBar) findViewById(R.id.seekBarTipSlider);
        normalTipTv = (TextView) findViewById(R.id.textViewNormalTip);
        billTv = (TextView) findViewById(R.id.editTextBill);
        toPayTv = (TextView) findViewById(R.id.textViewToPay);

        // Assign listeners whose methods are all located in this activity class
        spinnerSkill.setOnItemSelectedListener(this);
        spinnerCountry.setOnItemSelectedListener(this);
        radioGroupAvailability.setOnCheckedChangeListener(this);
        waiterTimer.setOnChronometerTickListener(this);
        tipSlider.setOnSeekBarChangeListener(this);
        billTv.addTextChangedListener(this);

        // Default to "Okay" problemsolvingskill
        spinnerSkill.setSelection(1);
        // Urge the user to measure the time to table
        waiterTimer.setText(R.string.timer_start_measuring);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.tip_timer_controls, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId())
        {
            // Start the timer from 0 in case the action button is pressed
            case R.id.action_timer_start:
                secondsWaited = 0;
                waiterTimer.setBase(SystemClock.elapsedRealtime());
                waiterTimer.start();
                return true;
            // Stop the time and deliver a judgement of the service speed
            case R.id.action_timer_pause:
                waiterTimer.stop();
                if (secondsWaited > 30)
                {
                    scoreSpeed = 0;
                    waiterTimer.setText(R.string.timer_slow_service);
                }
                else
                {
                    scoreSpeed = 1;
                    waiterTimer.setText(R.string.timer_fast_service);
                }
                // Method present in most listeners to show updated values in the user interface
                showResult(getTipFromScore());
                return true;
            // Abort the timing attempt and reset the waiter speed score
            case R.id.action_timer_reset:
                waiterTimer.stop();
                waiterTimer.setBase(SystemClock.elapsedRealtime());
                secondsWaited = 0;
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Mainly reflects value changes into the user interface
    private void showResult(double tipAmount)
    {
        // Decrease precision to match the rounding discrepancy from the seekbar
        tipPercentage = Math.floor(tipAmount * 100.0) / 100.0;
        setSliderPercent(tipAmount);
        printTip(tipAmount);
        printFinalBill();
    }

    // Print out a formatted text with 2 decimal places the final sum to pay, including tip.
    private void printFinalBill()
    {
        toPayTv.setText(String.format("%.2f",getTotalToPay()));
    }

    private void printTip(double tipAmount)
    {
        tippingTv.setText(String.format("+%.2f%%", tipAmount));
    }
    /*
     * Convert a percent double into a slider position
     * while also taking into consideration the slider minimum position
     */
    private void setSliderPercent(double percent)
    {
        tipSlider.setProgress((int) (percent * 100) - (tipPercentageMin * 100));
    }

    /*
     * Calculate how many percent tip each score is worth
     * then calculate the tip amount.
     */
    private double getTipFromScore()
    {
        double percentageScore = tipPercentageSpan / countScoreAreas;

        double tipAmount = tipPercentageMin + (percentageScore * calculateScoreTotal()) ;
        return tipAmount;
    }

    // Sum all scores, prevent negative tip because of poor service
    private int calculateScoreTotal()
    {
        scoreTotal = scoreNice + scoreAvailability + scoreProblemSolving + scoreRecommends + scoreSpecial + scoreSpeed ;

        if (scoreTotal > 0) { return scoreTotal; }
        else { return 0; }
    }

    // Find and completely setup the spinner
    private Spinner findSpinnerAdaptStringArray(int spinnerId, int stringArrayId)
    {
        // Spinner setup
        Spinner spinner = (Spinner) findViewById(spinnerId);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, stringArrayId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }
    // Define a valid tip range to be enforced
    private void setTipRange(int minPercentage, int maxPercentage)
    {
        tipPercentageMin = minPercentage;
        tipPercentageSpan = (double) maxPercentage - minPercentage;

        tipSlider.setMax((int) (tipPercentageSpan * 100));

        String countryDescription = String.format("%d - %d%% %s",minPercentage,maxPercentage,getResources().getString(R.string.forGoodService));
        normalTipTv.setText(countryDescription);
    }

    // This is how much should be payed in the end
    public double getTotalToPay()
    {
        if(!billTv.getText().toString().isEmpty())
        {
            String billString = billTv.getText().toString();
            double billNumber = Double.parseDouble(billString);
            double totalToPay = billNumber + (billNumber * (tipPercentage / 100.0));
            return totalToPay;
        }
        else
        {
            return 0;
        }
    }

    // Sets tiprange or scores problem solcing skill depending on which item is selected in a spinner
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId())
        {
            case R.id.spinnerCountry:
                switch (position)
                {
                    case ICELAND:
                        setTipRange(0, 10);
                        break;
                    case SWITZERLAND:
                        setTipRange(0,2);
                        break;
                    case GREECE:
                        setTipRange(5,10);
                        break;
                    case USA:
                        setTipRange(15,30);
                        break;
                }
            break;
            case R.id.spinnerSkill:
                switch (position)
                {
                    case COMPETENT: scoreProblemSolving = SCORE_GOOD;
                        break;
                    case MEDIOCRE: scoreProblemSolving = SCORE_OK;
                        break;
                    case INCOMPETENT: scoreProblemSolving = SCORE_BAD;
                        break;
                }
        }
        showResult(getTipFromScore());
    }
    // Assigns score depending on checked radio button
    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

        switch (checkedId)
        {
            case R.id.radioButtonGood:
                scoreAvailability = SCORE_GOOD;
                break;
            case R.id.radioButtonOk:
                scoreAvailability = SCORE_OK;
                break;
            case R.id.radioButtonBad:
                scoreAvailability = SCORE_BAD;
                break;
        }
        showResult(getTipFromScore());
    }

    // Assigns score for every checkbox clicked (or not)
    public void onCheckBoxClicked(View view) {

        //Säg först att den clickade vyn är en CheckBox så att isChecked() kan användas
        boolean checked = ((CheckBox) view).isChecked();

        // Spara 1 poäng om det är sant att rutan är kryssad, annars 0 poäng
        switch (view.getId())
        {
            case R.id.checkBoxFriendly:
                scoreNice = checked ? 1 : 0;
                break;
            case R.id.checkBoxSpecials:
                scoreSpecial = checked ? 1 : 0;
                break;
            case R.id.checkBoxRecommends:
                scoreRecommends = checked ? 1 : 0;
                break;
        }
        showResult(getTipFromScore());
    }

    // Chronometer ticks about every second, and saves the time passed
    @Override
    public void onChronometerTick(Chronometer chronometer)  {

        secondsWaited++;
    }

    // Update tip percentage when seekbar is changed
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        // Omvandla seekBarens progress till procent med decimaler
        tipPercentage = (double) progress / 100 + tipPercentageMin;

        if (fromUser){
            printTip(tipPercentage);
            printFinalBill();
        }
    }

    //Print final bill after entering food bill
    @Override
    public void afterTextChanged(Editable s) {

        printFinalBill();
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }



    @Override
    public void onNothingSelected(AdapterView<?> parent) {  }
}
