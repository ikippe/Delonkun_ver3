package jp.oida.delonkun3;

import java.util.ArrayList;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Process;
import android.speech.RecognizerIntent;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;


public class DelonkunActivity extends Activity 
{
	/*********
		定数
	**********/
	// リクエストコード
	private final static int REQUEST_CODE		= 0;

	private static final int WORD_KIND_STOP		= 0;
	private static final int WORD_KIND_FORWARD	= 1;
	private static final int WORD_KIND_BACK		= 2;
	private static final int WORD_KIND_RIGHT	= 3;
	private static final int WORD_KIND_LEFT		= 4;
	private static final int WORD_KIND_GO		= 5;

	private static final int TONE_CMD_LEFT			= ToneGenerator.TONE_DTMF_1;
	private static final int TONE_CMD_RIGHT			= ToneGenerator.TONE_DTMF_2;
	private static final int TONE_CMD_FORWARD		= ToneGenerator.TONE_DTMF_8;
	private static final int TONE_CMD_BACK			= ToneGenerator.TONE_DTMF_4;
	private static final int TONE_CMD_BRAKE			= ToneGenerator.TONE_DTMF_P;
	private static final int TONE_CMD_STOP			= ToneGenerator.TONE_DTMF_D;
	private static final int TONE_CMD_LEFT_FORWARD	= ToneGenerator.TONE_DTMF_9;
	private static final int TONE_CMD_LEFT_BACK		= ToneGenerator.TONE_DTMF_5;
	private static final int TONE_CMD_RIGHT_FORWARD	= ToneGenerator.TONE_DTMF_0;
	private static final int TONE_CMD_RIGHT_BACK	= ToneGenerator.TONE_DTMF_6;

	private static final int SEQ_CMD_NONE[] = {
		-1, -1
	};
	private static final int SEQ_CMD_STOP[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_BRAKE,			10},
		{TONE_CMD_STOP,				10},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_FORWARD[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_FORWARD,			500},
		{TONE_CMD_BACK,				50},
		{TONE_CMD_BRAKE,			10},
		{TONE_CMD_STOP,				10},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_BACK[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_BACK,				200},
		{TONE_CMD_FORWARD,			50},
		{TONE_CMD_BRAKE,			10},
		{TONE_CMD_STOP,				10},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_RIGHT[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_RIGHT,			500},
		{TONE_CMD_RIGHT_FORWARD,	500},
		{TONE_CMD_LEFT_BACK,		500},
		{TONE_CMD_RIGHT_FORWARD,	500},
		{TONE_CMD_LEFT_BACK,		500},
		{TONE_CMD_FORWARD,			50},
		{TONE_CMD_BRAKE,			10},
		{TONE_CMD_STOP,				10},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_LEFT[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_LEFT,				500},
		{TONE_CMD_LEFT_FORWARD,		500},
		{TONE_CMD_RIGHT_BACK,		500},
		{TONE_CMD_LEFT_FORWARD,		500},
		{TONE_CMD_RIGHT_BACK,		500},
		{TONE_CMD_FORWARD,			50},
		{TONE_CMD_BRAKE,			100},
		{TONE_CMD_STOP,				100},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_GO[][] = {
//		cmd,						wait(ms)
		{TONE_CMD_FORWARD,			1500},
		{TONE_CMD_BACK,				50},
		{TONE_CMD_BRAKE,			10},
		{TONE_CMD_STOP,				10},
		SEQ_CMD_NONE,
	};
	private static final int SEQ_CMD_TBL[][][] = {
		SEQ_CMD_STOP,
		SEQ_CMD_FORWARD,
		SEQ_CMD_BACK,
		SEQ_CMD_RIGHT,
		SEQ_CMD_LEFT,
		SEQ_CMD_GO,
	};
	private static final String MOVE_WORD[]	= {
		"top", 
		"前", 
		"しろ", 
		"右", 
		"左", 
		"け", 
	};

	/*********
		変数
	 *********/
	// トーンジェネレータ
	private ToneGenerator gToneGenerator;


	/***********
		初期化
	 ***********/
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		// トーンジェネレータを生成
		gToneGenerator	= new ToneGenerator(AudioManager.STREAM_SYSTEM, ToneGenerator.MAX_VOLUME);

		super.onCreate(savedInstanceState);
		
		WindowSleep.invalid(this);

		// 画面レイアウトの設定
		LinearLayout _layout	= new LinearLayout(this);
		_layout.setOrientation(LinearLayout.VERTICAL);
		setContentView(_layout);

		// ボタンを生成(音声識別用)
		Button _btnVoiceRec	= new Button(this);
		_btnVoiceRec.setText("Voice Recognition");
		_btnVoiceRec.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v)
			{
				ActivateRecognize();
			}
		});
		_layout.addView(_btnVoiceRec);

		// ボタンを生成(強制停止用)
		Button _btnStop	= new Button(this);
		_btnStop.setText("STOP!!!!!");
		_btnStop.setHeight(300);
		_btnStop.setOnClickListener(new View.OnClickListener() 
		{
			public void onClick(View v)
			{
				MachineMoveSequence(SEQ_CMD_TBL[WORD_KIND_STOP]);
			}
		});
		_layout.addView(_btnStop);

		// ストップ命令発行
		MachineMoveSequence(SEQ_CMD_TBL[WORD_KIND_STOP]);
	}

	/*************************
		音声を判別し動作を決める
	 *************************/
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if ( requestCode == REQUEST_CODE && resultCode == RESULT_OK ) 
		{
			int _wordKind = 0xFF;
			
			// 音声識別開始
			ArrayList<String> results	= data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
			String _resultsString	= "";
			for ( int i = 0; i < results.size(); ++i ) 
			{
				_resultsString += results.get(i);
			}

			// 識別内容により鳴らすトーンを決める
			if (_resultsString.contains(MOVE_WORD[WORD_KIND_RIGHT]) )		// 右
			{
				_wordKind = WORD_KIND_RIGHT;
			}
			else 
			if ( _resultsString.contains(MOVE_WORD[WORD_KIND_LEFT]) ) 		// 左
			{
				_wordKind = WORD_KIND_LEFT;
			}
			else
			if ( _resultsString.contains(MOVE_WORD[WORD_KIND_FORWARD]) )	// 前
			{
				_wordKind = WORD_KIND_FORWARD;
			}
			else 
			if ( _resultsString.contains(MOVE_WORD[WORD_KIND_BACK]) )		// 後
			{
				_wordKind = WORD_KIND_BACK;
			}
			else 
			if ( _resultsString.contains(MOVE_WORD[WORD_KIND_GO]) )			// いけ
			{
				_wordKind = WORD_KIND_GO;
			}

			// シーケンスの実行
			if (_wordKind != 0xFF)
			{
				MachineMoveSequence(SEQ_CMD_TBL[_wordKind]);
			}

			// 認識内容を表示
			TorstMessage(_wordKind, _resultsString);

			// 再度音声識別スタート
			ActivateRecognize();
		}
		super.onActivityResult(requestCode, resultCode, data);
	}
	
	/***********************
		終了時のバックキーの処理
	 ***********************/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		if(keyCode != KeyEvent.KEYCODE_BACK)
		{
			// アプリを終了する
			this.finish();
			Process.killProcess(Process.myPid());
		}
		return super.onKeyDown(keyCode, event);
	}

	/**************************************
		音声認識用インテントの発行(音声認識開始)
	 **************************************/
	private void ActivateRecognize()
	{
		try
		{
			Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
			intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
			intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Delon-kun Voice Recognition");
			startActivityForResult(intent, REQUEST_CODE);
		}
		catch( ActivityNotFoundException e )
		{
			e.printStackTrace();
		}
	}
	
	/***********************
		DTMFデータを端子に出力
	 ***********************/
	public void PushTone(int aToneKind, int aSleepTime)
	{
		// トーンを鳴らす
		gToneGenerator.startTone(aToneKind);
		try{Thread.sleep(32);}catch(InterruptedException e){e.printStackTrace();}
		gToneGenerator.stopTone();
		try{Thread.sleep(aSleepTime);}catch(InterruptedException e){e.printStackTrace();}
	}
	
	/***************************
		トーストで端末認識内容を確認
	 ***************************/
	private void TorstMessage(int aWordKind, String aResultString)
	{
		String _msg = "";
		if ( aWordKind == 0xFF )
		{
			_msg = "【誤認識】";
		}
		else
		{
			_msg = "【"+MOVE_WORD[aWordKind]+"】";
		}
		_msg += aResultString;
		Toast.makeText(this, _msg, Toast.LENGTH_LONG).show();
	}
	
	/***************************
		マシンの動作用シーケンス処理
	 ***************************/
	void MachineMoveSequence(int seq[][])
	{
		int seq_ix = 0;
		while (seq[seq_ix][0] != -1)
		{
		  	PushTone(seq[seq_ix][0], seq[seq_ix][1]);
		  	seq_ix++;
		}
	}
}
/****************************** end of file ******************************/