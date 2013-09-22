package jp.oida.delonkun3;


import android.app.Activity;
import android.view.WindowManager;



/***********************

		スリープクラス

 ***********************/
public class WindowSleep
{
	/*******************
		スリープを無効にする
	********************/
	public static void invalid(Activity act)
	{
		act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	/*******************
		スリープを有効にする
	********************/
	public static void valid(Activity act)
	{
		act.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}
}