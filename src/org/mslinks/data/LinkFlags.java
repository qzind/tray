/*
	https://github.com/BlackOverlord666/org.mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package org.mslinks.data;

import org.mslinks.io.ByteReader;

import java.io.IOException;

public class LinkFlags extends BitSet32 {
	
	public LinkFlags(int n) {
		super(n);
		reset();
	}
	
	public LinkFlags(ByteReader data) throws IOException {
		super(data);
		reset();
	}
	
	private void reset() {
		clear(11);
		clear(16);
		for (int i=27; i<32; i++)
			clear(i);
	}
	
	public boolean hasLinkTargetIDList() 			{ return get(0); }	
	public boolean hasLinkInfo() 					{ return get(1); }	
	public boolean hasName() 						{ return get(2); }	
	public boolean hasRelativePath() 				{ return get(3); }	
	public boolean hasWorkingDir() 					{ return get(4); }	
	public boolean hasArguments() 					{ return get(5); }	
	public boolean hasIconLocation() 				{ return get(6); }	
	public boolean isUnicode() 						{ return get(7); }	
	public boolean forceNoLinkInfo() 				{ return get(8); }	
	public boolean hasExpString() 					{ return get(9); }	
	public boolean runInSeparateProcess() 			{ return get(10); }	
	public boolean hasDarwinID() 					{ return get(12); }	
	public boolean runAsUser() 						{ return get(13); }	
	public boolean hasExpIcon() 					{ return get(14); }	
	public boolean noPidlAlias() 					{ return get(15); }	
	public boolean runWithShimLayer() 				{ return get(17); }	
	public boolean forceNoLinkTrack() 				{ return get(18); }	
	public boolean enableTargetMetadata() 			{ return get(19); }	
	public boolean disableLinkPathTracking() 		{ return get(20); }	
	public boolean disableKnownFolderTracking() 	{ return get(21); }	
	public boolean disableKnownFolderAlias() 		{ return get(22); }	
	public boolean allowLinkToLink() 				{ return get(23); }	
	public boolean unaliasOnSave() 					{ return get(24); }	
	public boolean preferEnvironmentPath() 			{ return get(25); }	
	public boolean keepLocalIDListForUNCTarget() 	{ return get(26); }
	
	public LinkFlags setHasLinkTargetIDList() 			{ set(0); return this; }	
	public LinkFlags setHasLinkInfo() 					{ set(1); return this; }	
	public LinkFlags setHasName() 						{ set(2); return this; }	
	public LinkFlags setHasRelativePath() 				{ set(3); return this; }	
	public LinkFlags setHasWorkingDir() 					{ set(4); return this; }	
	public LinkFlags setHasArguments() 					{ set(5); return this; }	
	public LinkFlags setHasIconLocation() 				{ set(6); return this; }	
	public LinkFlags setIsUnicode() 						{ set(7); return this; }	
	public LinkFlags setForceNoLinkInfo() 				{ set(8); return this; }	
	public LinkFlags setHasExpString() 					{ set(9); return this; }	
	public LinkFlags setRunInSeparateProcess() 			{ set(10); return this; }	
	public LinkFlags setHasDarwinID() 					{ set(12); return this; }	
	public LinkFlags setRunAsUser() 						{ set(13); return this; }	
	public LinkFlags setHasExpIcon() 					{ set(14); return this; }	
	public LinkFlags setNoPidlAlias() 					{ set(15); return this; }	
	public LinkFlags setRunWithShimLayer() 				{ set(17); return this; }	
	public LinkFlags setForceNoLinkTrack() 				{ set(18); return this; }	
	public LinkFlags setEnableTargetMetadata() 			{ set(19); return this; }	
	public LinkFlags setDisableLinkPathTracking() 		{ set(20); return this; }	
	public LinkFlags setDisableKnownFolderTracking() 	{ set(21); return this; }	
	public LinkFlags setDisableKnownFolderAlias() 		{ set(22); return this; }	
	public LinkFlags setAllowLinkToLink() 				{ set(23); return this; }	
	public LinkFlags setUnaliasOnSave() 					{ set(24); return this; }	
	public LinkFlags setPreferEnvironmentPath() 			{ set(25); return this; }	
	public LinkFlags setKeepLocalIDListForUNCTarget() 	{ set(26); return this; }
	
	public LinkFlags clearHasLinkTargetIDList() 			{ clear(0); return this; }	
	public LinkFlags clearHasLinkInfo() 					{ clear(1); return this; }	
	public LinkFlags clearHasName() 						{ clear(2); return this; }	
	public LinkFlags clearHasRelativePath() 				{ clear(3); return this; }	
	public LinkFlags clearHasWorkingDir() 				{ clear(4); return this; }	
	public LinkFlags clearHasArguments() 				{ clear(5); return this; }	
	public LinkFlags clearHasIconLocation() 				{ clear(6); return this; }	
	public LinkFlags clearIsUnicode() 					{ clear(7); return this; }	
	public LinkFlags clearForceNoLinkInfo() 				{ clear(8); return this; }	
	public LinkFlags clearHasExpString() 				{ clear(9); return this; }	
	public LinkFlags clearRunInSeparateProcess() 		{ clear(10); return this; }	
	public LinkFlags clearHasDarwinID() 					{ clear(12); return this; }	
	public LinkFlags clearRunAsUser() 					{ clear(13); return this; }	
	public LinkFlags clearHasExpIcon() 					{ clear(14); return this; }	
	public LinkFlags clearNoPidlAlias() 					{ clear(15); return this; }	
	public LinkFlags clearRunWithShimLayer() 			{ clear(17); return this; }	
	public LinkFlags clearForceNoLinkTrack() 			{ clear(18); return this; }	
	public LinkFlags clearEnableTargetMetadata() 		{ clear(19); return this; }	
	public LinkFlags clearDisableLinkPathTracking() 		{ clear(20); return this; }	
	public LinkFlags clearDisableKnownFolderTracking() 	{ clear(21); return this; }	
	public LinkFlags clearDisableKnownFolderAlias() 		{ clear(22); return this; }	
	public LinkFlags clearAllowLinkToLink() 				{ clear(23); return this; }	
	public LinkFlags clearUnaliasOnSave() 				{ clear(24); return this; }	
	public LinkFlags clearPreferEnvironmentPath() 		{ clear(25); return this; }	
	public LinkFlags clearKeepLocalIDListForUNCTarget() 	{ clear(26); return this; }
}
