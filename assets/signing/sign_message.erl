%%%-------------------------------------------------------------------
%%% #########################################################
%%% #             WARNING   WARNING   WARNING               #
%%% #########################################################
%%% #                                                       #
%%% # This file is intended for demonstration purposes      #
%%% # only.                                                 #
%%% #                                                       #
%%% # It is the SOLE responsibility of YOU, the programmer  #
%%% # to prevent against unauthorized access to any signing #
%%% # functions.                                            #
%%% #                                                       #
%%% # Organizations that do not protect against un-         #
%%% # authorized signing will be black-listed to prevent    #
%%% # software piracy.                                      #
%%% #                                                       #
%%% # -QZ Industries, LLC                                   #
%%% #                                                       #
%%% #########################################################
%%%-------------------------------------------------------------------
-module(sign_message).
-export([sign/2]).

%%%
%%% Usage:
%%%    -import(sign_message, [sign/2]).
%%%    [...]
%%%    sign_message:sign(GetRequest, "path/to/private-key.rsa").
%%%       * Where GetRequest is the the "foo" portion of "?request=foo"
%%%       * Web framework must echo the base64 encoded signature in plain text
%%%       * Browser must use ajax technique to fetch base64 signature
%%%       * See also qz.api.setSignaturePromise(...)
%%%
%%% Important:
%%%    * Private key MUST be converted to newer RSA format using the following command:
%%%       openssl rsa -in "private-key.pem" -out "private-key.rsa"
%%%
%%% Watch for:
%%%   badmatch,{error,enoent} key cannot be read; check for valid path
%%%   function_clause,[{public_key,sign,...}] key must be converted to newer RSA format
%%%
%%%

sign(Message, KeyPath) ->
  {ok, Data} = file:read_file(KeyPath),
  [KeyEntry] =  public_key:pem_decode(Data),
  PrivateKey = public_key:pem_entry_decode(KeyEntry),
  Signature = public_key:sign(list_to_binary(Message), sha, PrivateKey),
  Base64 = base64:encode(Signature),
  io:fwrite(Base64).
