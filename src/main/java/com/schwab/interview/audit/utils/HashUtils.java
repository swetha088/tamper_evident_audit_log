package com.schwab.interview.audit.utils;
import java.nio.charset.StandardCharsets; import java.security.*; import java.util.HexFormat;
public final class HashUtils { private HashUtils(){} public static String sha256(String input){try{return HexFormat.of().formatHex(MessageDigest.getInstance(AuditConstants.SHA_256).digest(input.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 is unavailable",e);}} }
