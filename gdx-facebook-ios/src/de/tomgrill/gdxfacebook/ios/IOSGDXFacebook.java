/*******************************************************************************
 * Copyright 2015 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package de.tomgrill.gdxfacebook.ios;

import java.util.Collection;
import java.util.List;

import org.robovm.apple.foundation.NSError;
import org.robovm.objc.block.VoidBlock2;
import org.robovm.pods.facebook.core.FBSDKAccessToken;
import org.robovm.pods.facebook.login.FBSDKLoginBehavior;
import org.robovm.pods.facebook.login.FBSDKLoginManager;
import org.robovm.pods.facebook.login.FBSDKLoginManagerLoginResult;

import com.badlogic.gdx.utils.Array;

import de.tomgrill.gdxfacebook.core.GDXFacebook;
import de.tomgrill.gdxfacebook.core.GDXFacebookAccessToken;
import de.tomgrill.gdxfacebook.core.GDXFacebookCallback;
import de.tomgrill.gdxfacebook.core.GDXFacebookConfig;
import de.tomgrill.gdxfacebook.core.GDXFacebookError;
import de.tomgrill.gdxfacebook.core.GDXFacebookLoginResult;

public class IOSGDXFacebook extends GDXFacebook {

	private FBSDKLoginManager loginManager;

	private GDXFacebookAccessToken accessToken;

	public IOSGDXFacebook(GDXFacebookConfig config) {
		super(config);

		loginManager = new FBSDKLoginManager();
		loginManager.setLoginBehavior(FBSDKLoginBehavior.Native);
	}

	@Override
	public void loginWithReadPermissions(Collection<String> permissions, GDXFacebookCallback<GDXFacebookLoginResult> callback) {
		login(permissions, callback, false);
	}

	@Override
	public void loginWithPublishPermissions(Collection<String> permissions, GDXFacebookCallback<GDXFacebookLoginResult> callback) {
		login(permissions, callback, true);
	}

	private void login(Collection<String> permissions, final GDXFacebookCallback<GDXFacebookLoginResult> callback, boolean withPublishPermissions) {

		/**
		 * Note: ios facebook SDK does not check for installed Facebook App.
		 */

		if (FBSDKAccessToken.getCurrentAccessToken() != null && arePermissionsGranted(permissions)) {

			GDXFacebookLoginResult result = new GDXFacebookLoginResult();

			accessToken = toGDXFacebookToken(FBSDKAccessToken.getCurrentAccessToken());
			storeToken(accessToken);

			result.setAccessToken(accessToken);
			callback.onSuccess(result);

		} else {

			VoidBlock2<FBSDKLoginManagerLoginResult, NSError> bloack = new VoidBlock2<FBSDKLoginManagerLoginResult, NSError>() {

				@Override
				public void invoke(FBSDKLoginManagerLoginResult loginResult, NSError nsError) {

					if (nsError != null) {
						accessToken = null;
						storeToken(accessToken);
						GDXFacebookError error = new GDXFacebookError();
						error.setErrorMessage(nsError.getLocalizedDescription());
						callback.onError(error);

					} else if (loginResult.isCancelled()) {
						accessToken = null;
						storeToken(accessToken);
						callback.onCancel();
					} else {
						GDXFacebookLoginResult result = new GDXFacebookLoginResult();

						accessToken = toGDXFacebookToken(FBSDKAccessToken.getCurrentAccessToken());
						storeToken(accessToken);
						result.setAccessToken(accessToken);
						callback.onSuccess(result);
					}

				}

			};

			List<String> listPermissions = (List<String>) permissions;

			if (withPublishPermissions) {
				loginManager.logInWithPublishPermissions(listPermissions, bloack);
			} else {
				loginManager.logInWithReadPermissions(listPermissions, bloack);
			}
		}

	}

	@Override
	public boolean isLoggedIn() {
		return accessToken != null;
	}

	@Override
	public void logOut() {
		accessToken = null;
		storeToken(accessToken);
		loginManager.logOut();
	}

	@Override
	public GDXFacebookAccessToken getAccessToken() {
		return accessToken;
	}

	private GDXFacebookAccessToken toGDXFacebookToken(FBSDKAccessToken accessToken) {
		return new GDXFacebookAccessToken(accessToken.getTokenString(), accessToken.getAppID(), accessToken.getUserID(), collectionToGdxArray(accessToken.getPermissions()),
				collectionToGdxArray(accessToken.getDeclinedPermissions()), accessToken.getExpirationDate().toDate().getTime(), accessToken.getRefreshDate().toDate().getTime());
	}

	// private GDXFacebookAccessToken loadAccessToken() {
	// String accessTokenAsJson = prefs.getString("accessTokenAsJson", null);
	// if (accessTokenAsJson == null) {
	// return null;
	// }
	// System.out.println(accessTokenAsJson);
	// Json json = new Json();
	// json.setOutputType(OutputType.json);
	// return json.fromJson(GDXFacebookAccessToken.class, accessTokenAsJson);
	// }

	// private Collection<String> gdxArrayToCollection(Array<String> array) {
	// Collection<String> col = new ArrayList<String>();
	// for (int i = 0; i < array.size; i++) {
	// col.add(array.get(i));
	// }
	// return col;
	// }

	private Array<String> collectionToGdxArray(Collection<String> col) {
		String[] arr = new String[col.size()];
		col.toArray(arr);
		return new Array<String>(arr);
	}

}
