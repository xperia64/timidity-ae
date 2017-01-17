/*******************************************************************************
 * Copyright (C) 2017 xperia64 <xperiancedapps@gmail.com>
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.xperia64.timidityae;

import android.app.Activity;
import android.os.Bundle;

// @formatter:off
/*
           ,Mk      ,MMMMMMMMMMMk
           .:cddddddOMMkooooKM0oodd.
             ,MMMMKxxxd;.;dxXMx.;dxol.
           .:cddXM0oc..cokMMOd:....OM,
         ..:Kd..OMMM0::0MMMMc......OM,
         kMk ,MMMMMMMMMMMMMMMMl  ..OM,
      :00l:' ,MMMMd.cMMc.cMMMMl  ..OM,
    lOd::,....''''lOx::xOd''''..:KKc'
 .ddoo:...........;oc..coodd,.:kd::.
ddoo,....................lMM0kKMk
MO..................;ddddOMMMMMMKll.
ddoo;...........coookMMMMMMMMMKk0MM,
 ,MMl...........OMMMMMMMMMMMMMd.lMM,
  ..xXXXXXXXXXXKWMMMMMMMM0::::,.lMM,
    ............OMMM0::::;......lMM,
             'OOo:::;...........,::xO'
           .ddoo,..................OM,
           ,MO.....................OM,
           ,MKoo;......coc......;ooxkl::
         ..:KKKKo::::::0M0::::::oKKc.OMM
         kMO....OMMMMMMMMMMMMMMMO....OMM
         kMO....;::::::0M0::::::,....OMM
         kMO...........;:;...........OMM
         kMO.........................OMM
         ':lkkkk:.............ckkkkkkl::
           .llllddddddddddddddollllll.
                :dddOMMMMMMMMMl
                    ,MMMMMMMMMl
                    ,MMMM0::0Ml
             '000000XMMd:,..OMN000000'
           'Ox:::::::::,....;::::::::xOO
           ,MNkkkkkkkkkkkkkkkkkkkkkkkNMM
           ,MMMMMMMMMMMMMMMMMMMMMMMMMMMM
*/
// @formatter:on
public class DummyActivity extends Activity {
	@Override
	public void onCreate(Bundle potato) {
		// Because JB's/KK's/LP's task handling is dumb.
		// Really Google? This bug is still present in Lollipop.
		// I should not have to create an Activity just to keep my service alive.

		// This appears to be fixed in MM
		super.onCreate(potato);
		// With and without this something breaks. 
		// With, it closes the task manager. 
		// Without, volume buttons break and lag occurs.
		this.finish();
	}
}