package kr.hyosang.drivediary.client;

import kr.hyosang.drivediary.client.R;
import kr.hyosang.drivediary.client.database.DbHelper;
import kr.hyosang.drivediary.client.network.UploadThread;
import android.app.Dialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;


public class FuelDialog extends Dialog {
	private Context mContext;
	private TextView mTxtPos;
	private EditText mTxtOdo;
	private EditText mTxtUnitPrice;
	private EditText mTxtTotalPrice;
	private EditText mTxtLiter;
	private Button mBtnConfirm;
	private Button mBtnCancel;
	private CheckBox mChkFull;
	private boolean isChangeLock = false;
	
	private String mPosStr;
	
	
	public FuelDialog(Context context, String posData) {
		super(context);
		
		mContext = context;
		mPosStr = posData;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setTitle("주유기록 추가");
		
		setContentView(R.layout.fuel_dialog);
		
		mTxtPos = (TextView)findViewById(R.id.txtPos);
		mTxtOdo = (EditText)findViewById(R.id.txtOdo);
		mTxtUnitPrice = (EditText)findViewById(R.id.txtUnitPrice);
		mTxtLiter = (EditText)findViewById(R.id.txtLiter);
		mTxtTotalPrice = (EditText)findViewById(R.id.txtTotalPrice);
		mBtnConfirm = (Button)findViewById(R.id.btn_confirm);
		mBtnCancel = (Button)findViewById(R.id.btn_cancel);
		mChkFull = (CheckBox)findViewById(R.id.chkFull);
		
		mTxtOdo.addTextChangedListener(mAutoCalc);
		mTxtUnitPrice.addTextChangedListener(mAutoCalc);
		mTxtTotalPrice.addTextChangedListener(mAutoCalc);
		mTxtLiter.addTextChangedListener(mAutoCalc);
		
		mTxtLiter.setOnEditorActionListener(mEditorAction);
		
		mBtnConfirm.setOnClickListener(mButtonClick);
		mBtnCancel.setOnClickListener(mButtonClick);
		
		mTxtPos.setText(mPosStr);
		
		Log.d("DATA", mPosStr);
		
	}
	
	private View.OnClickListener mButtonClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if(v.getId() == mBtnConfirm.getId()) {
				//confirm
			    /*
				Intent i = new Intent(mContext, FuelUploadService.class);
				i.putExtra(FuelUploadService.EXTRA_ISFULL, mChkFull.isChecked() ? "Y" : "N");
				i.putExtra(FuelUploadService.EXTRA_LITER, mTxtLiter.getText().toString());
				i.putExtra(FuelUploadService.EXTRA_ODO, mTxtOdo.getText().toString());
				i.putExtra(FuelUploadService.EXTRA_POS, mPosStr);
				i.putExtra(FuelUploadService.EXTRA_TOTAL_PRICE, mTxtTotalPrice.getText().toString());
				i.putExtra(FuelUploadService.EXTRA_UNIT_PRICE, mTxtUnitPrice.getText().toString());
				
				mContext.startService(i);
				*/
			    
			    DbHelper db = new DbHelper(mContext);
			    
			    long res = db.addFuelRecord(mPosStr,
			            Util.parseInt(mTxtOdo.getText().toString(), -1),
			            Util.parseInt(mTxtUnitPrice.getText().toString(), -1),
			            Util.parseInt(mTxtTotalPrice.getText().toString(), -1),
			            Util.parseDouble(mTxtLiter.getText().toString(), -1),
			            mChkFull.isChecked()
			            );
			    
			    Toast.makeText(mContext, "Fuel record insert = " + res, Toast.LENGTH_SHORT).show();
			    
			    WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
			    if(wifiManager.getConnectionInfo().getIpAddress() != 0) {
			        //업로드 스레드 시작
			        (new UploadThread(mContext)).start();
			    }
			    
				dismiss();
			}else if(v.getId() == mBtnCancel.getId()) {
				dismiss();
			}
		}
	};
	
	private OnEditorActionListener mEditorAction = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			if(actionId == EditorInfo.IME_ACTION_DONE) {
				InputMethodManager imm = (InputMethodManager)mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.toggleSoftInput(0, 0);
				
				return true;
			}
			return false;
		}
	};
	
	private TextWatcher mAutoCalc = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable editable) {
			if(!isChangeLock) { 
				isChangeLock = true;
				if(mTxtLiter.hasFocus()) {
					calcTotalPrice();
				}else if(mTxtTotalPrice.hasFocus()) {
					calcLiter();
				}else if(mTxtUnitPrice.hasFocus()) {
					mTxtLiter.setText("0");
					mTxtTotalPrice.setText("0");
				}
				isChangeLock = false;
			}
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2,
				int arg3) {
			
		}
	};
	
	/*
	private View.OnKeyListener mKeyListener = new View.OnKeyListener() {
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if(event.getAction() == KeyEvent.ACTION_UP) {
				if(v.getId() == mTxtLiter.getId()) {
					calcTotalPrice();
				}else if(v.getId() == mTxtTotalPrice.getId()) {
					calcLiter();
				}else if(v.getId() == mTxtUnitPrice.getId()) {
					mTxtLiter.setText("0");
					mTxtTotalPrice.setText("0");
				}
				
				
			}
			return false;
		}
	};
	*/
	
	private void calcLiter() {
		double unitPrice = getDouble(mTxtUnitPrice);
		double totalPrice = getDouble(mTxtTotalPrice);
		
		double liter = (totalPrice / unitPrice);
		
		mTxtLiter.setText(String.format("%.2f", liter));
	}
	
	private void calcTotalPrice() {
		double unitPrice = getDouble(mTxtUnitPrice);
		double liter = getDouble(mTxtLiter);
		
		int totalPrice = (int)(unitPrice * liter);
		
		mTxtTotalPrice.setText(String.valueOf(totalPrice));
	}
	
	private double getDouble(EditText view) {
		String txt = view.getText().toString();
		try {
			return Double.parseDouble(txt);
		}catch(NumberFormatException e) {
			return 0;
		}
		
	}

}
