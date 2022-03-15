package me.ayunami2000.MakiDesktop;

import com.shinyhut.vernacular.client.VernacularClient;
import com.shinyhut.vernacular.client.VernacularConfig;
import com.shinyhut.vernacular.client.rendering.ColorDepth;
import org.bukkit.block.Block;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class VideoCaptureVnc extends Thread {
    public boolean running = true;
    private boolean mouseDown = false;

    public void onFrame(BufferedImage frame) { }

    private static Pattern ipPortPattern = Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}|localhost):?([0-9]{1,5})?");
    private static VernacularConfig config = new VernacularConfig();
    private static VernacularClient client = new VernacularClient(config);
    private static int screenWidth=0;
    private static int screenHeight=0;
    private static int rendering=0;
    private static int cachex=0;
    private static int cachey=0;

    public static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage&&MakiDesktop.colorOrder[0]==0&&MakiDesktop.colorOrder[1]==1&&MakiDesktop.colorOrder[2]==2&&MakiDesktop.colorOrder[3]==3)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);

        if(MakiDesktop.colorOrder[0]!=0||MakiDesktop.colorOrder[1]!=1||MakiDesktop.colorOrder[2]!=2||MakiDesktop.colorOrder[3]!=3) {
            for (int i = 0; i < bimage.getWidth(); i++) {
                for (int j = 0; j < bimage.getHeight(); j++) {
                    Color origColor=new Color(bimage.getRGB(i, j));
                    int[] oldRgb = new int[]{origColor.getRed(), origColor.getGreen(), origColor.getBlue(), origColor.getAlpha()};

                    int[] newRgb = new int[]{0,0,0,0};
                    for (int r = 0; r < newRgb.length; r++) {
                        newRgb[r]=oldRgb[MakiDesktop.colorOrder[r]];
                    }
                    bimage.setRGB(i, j, new Color(newRgb[0], newRgb[1], newRgb[2], newRgb[3]).getRGB());
                }
            }
        }

        bGr.dispose();

        // Return the buffered image
        return bimage;
    }

    {
        config.setColorDepth(ColorDepth.BPP_24_TRUE);
        config.setErrorListener(e -> {
            System.out.println(e);
            client.stop();
            MakiDesktop.paused = true;
        });

        //config.setTargetFramesPerSecond(20);

        config.setShared(true);

        config.setUsernameSupplier(() -> MakiDesktop.getUserPass()[0]);
        config.setPasswordSupplier(() -> MakiDesktop.getUserPass()[1]);

        config.setScreenUpdateListener(image -> {
            if (MakiDesktop.paused||rendering > 5) return;//dont get too behind
            rendering++;

            int oldWidth=screenWidth;
            int oldHeight=screenHeight;

            screenWidth=image.getWidth(null);
            screenHeight=image.getHeight(null);

            if(screenWidth!=oldWidth)cachex=screenWidth/2;
            if(screenHeight!=oldHeight)cachey=screenHeight/2;

            onFrame(toBufferedImage(image));

            try {
                Thread.sleep(200);//frame delay
            } catch (InterruptedException e) {
            }
            rendering--;
        });
    }

    public void run() {
        while(this.isAlive()&&this.running){
            Matcher m = ipPortPattern.matcher(ConfigFile.getIp());
            if (!m.find()) {
                System.out.println("Error: Expected IP:PORT");
                MakiDesktop.paused = true;
            }else {
                String ip = m.group(1),
                        port = m.group(2);
                client.stop();
                client.start(ip, Integer.parseInt(port));
                (new Thread(MakiDesktop.audioPlayer)).start();
                while (this.running&&!MakiDesktop.paused) {
                    if(MakiDesktop.controller!=null){
                        if(!MakiDesktop.controller.isOnline()){
                            MakiDesktop.controller=null;
                        }else if(MakiDesktop.alwaysMoveMouse) {
                            Block tgtbl = MakiDesktop.controller.getTargetBlock(5);
                            if (tgtbl != null) ClickOnScreen.clickedOnBlock(tgtbl, MakiDesktop.controller, false);
                        }
                    }
                    if(!MakiDesktop.audioPlayer.isEnabled())(new Thread(MakiDesktop.audioPlayer)).start();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                }
                MakiDesktop.audioPlayer.stopIt();
                if(!this.running)break;
                client.stop();
            }
            do {
                //sleep for some time
                try {
                    Thread.sleep(MakiDesktop.paused ? 1000 : ConfigFile.getDelay());
                } catch (InterruptedException e) {
                }
            } while (MakiDesktop.paused&&this.running);
        }
    }

    public void cleanup() {
        client.stop();
        running = false;
        Thread.currentThread().interrupt();//will THIS work???????
    }

    public void clickMouse(double x,double y,int doClick,boolean drag){
        if(x<0&&y<0){
            cachex+=x+32767;
            cachey+=y+32767;
            x=cachex;
            y=cachey;
        }
        x=Math.max(0,Math.min(screenWidth,x));
        y=Math.max(0,Math.min(screenHeight,y));
        client.moveMouse((int) (x*screenWidth), (int) (y*screenHeight));
        if(doClick!=0){
            if(drag){
                mouseDown=!mouseDown;
                client.updateMouseButton(doClick,mouseDown);
            }else{
                client.click(doClick);
            }
        }
    }

    private int keyNameToKeySym(String keyname){
        if(keyname.equals("space"))return 0x0020;
        if(keyname.equals("exclam"))return 0x0021;
        if(keyname.equals("quotedbl"))return 0x0022;
        if(keyname.equals("numbersign"))return 0x0023;
        if(keyname.equals("dollar"))return 0x0024;
        if(keyname.equals("percent"))return 0x0025;
        if(keyname.equals("ampersand"))return 0x0026;
        if(keyname.equals("quoteright"))return 0x0027;
        if(keyname.equals("parenleft"))return 0x0028;
        if(keyname.equals("parenright"))return 0x0029;
        if(keyname.equals("asterisk"))return 0x002a;
        if(keyname.equals("plus"))return 0x002b;
        if(keyname.equals("comma"))return 0x002c;
        if(keyname.equals("minus"))return 0x002d;
        if(keyname.equals("period"))return 0x002e;
        if(keyname.equals("slash"))return 0x002f;
        if(keyname.equals("0"))return 0x0030;
        if(keyname.equals("1"))return 0x0031;
        if(keyname.equals("2"))return 0x0032;
        if(keyname.equals("3"))return 0x0033;
        if(keyname.equals("4"))return 0x0034;
        if(keyname.equals("5"))return 0x0035;
        if(keyname.equals("6"))return 0x0036;
        if(keyname.equals("7"))return 0x0037;
        if(keyname.equals("8"))return 0x0038;
        if(keyname.equals("9"))return 0x0039;
        if(keyname.equals("colon"))return 0x003a;
        if(keyname.equals("semicolon"))return 0x003b;
        if(keyname.equals("less"))return 0x003c;
        if(keyname.equals("equal"))return 0x003d;
        if(keyname.equals("greater"))return 0x003e;
        if(keyname.equals("question"))return 0x003f;
        if(keyname.equals("at"))return 0x0040;
        if(keyname.equals("A"))return 0x0041;
        if(keyname.equals("B"))return 0x0042;
        if(keyname.equals("C"))return 0x0043;
        if(keyname.equals("D"))return 0x0044;
        if(keyname.equals("E"))return 0x0045;
        if(keyname.equals("F"))return 0x0046;
        if(keyname.equals("G"))return 0x0047;
        if(keyname.equals("H"))return 0x0048;
        if(keyname.equals("I"))return 0x0049;
        if(keyname.equals("J"))return 0x004a;
        if(keyname.equals("K"))return 0x004b;
        if(keyname.equals("L"))return 0x004c;
        if(keyname.equals("M"))return 0x004d;
        if(keyname.equals("N"))return 0x004e;
        if(keyname.equals("O"))return 0x004f;
        if(keyname.equals("P"))return 0x0050;
        if(keyname.equals("Q"))return 0x0051;
        if(keyname.equals("R"))return 0x0052;
        if(keyname.equals("S"))return 0x0053;
        if(keyname.equals("T"))return 0x0054;
        if(keyname.equals("U"))return 0x0055;
        if(keyname.equals("V"))return 0x0056;
        if(keyname.equals("W"))return 0x0057;
        if(keyname.equals("X"))return 0x0058;
        if(keyname.equals("Y"))return 0x0059;
        if(keyname.equals("Z"))return 0x005a;
        if(keyname.equals("bracketleft"))return 0x005b;
        if(keyname.equals("backslash"))return 0x005c;
        if(keyname.equals("bracketright"))return 0x005d;
        if(keyname.equals("asciicircum"))return 0x005e;
        if(keyname.equals("underscore"))return 0x005f;
        if(keyname.equals("quoteleft"))return 0x0060;
        if(keyname.equals("a"))return 0x0061;
        if(keyname.equals("b"))return 0x0062;
        if(keyname.equals("c"))return 0x0063;
        if(keyname.equals("d"))return 0x0064;
        if(keyname.equals("e"))return 0x0065;
        if(keyname.equals("f"))return 0x0066;
        if(keyname.equals("g"))return 0x0067;
        if(keyname.equals("h"))return 0x0068;
        if(keyname.equals("i"))return 0x0069;
        if(keyname.equals("j"))return 0x006a;
        if(keyname.equals("k"))return 0x006b;
        if(keyname.equals("l"))return 0x006c;
        if(keyname.equals("m"))return 0x006d;
        if(keyname.equals("n"))return 0x006e;
        if(keyname.equals("o"))return 0x006f;
        if(keyname.equals("p"))return 0x0070;
        if(keyname.equals("q"))return 0x0071;
        if(keyname.equals("r"))return 0x0072;
        if(keyname.equals("s"))return 0x0073;
        if(keyname.equals("t"))return 0x0074;
        if(keyname.equals("u"))return 0x0075;
        if(keyname.equals("v"))return 0x0076;
        if(keyname.equals("w"))return 0x0077;
        if(keyname.equals("x"))return 0x0078;
        if(keyname.equals("y"))return 0x0079;
        if(keyname.equals("z"))return 0x007a;
        if(keyname.equals("braceleft"))return 0x007b;
        if(keyname.equals("bar"))return 0x007c;
        if(keyname.equals("braceright"))return 0x007d;
        if(keyname.equals("asciitilde"))return 0x007e;
        if(keyname.equals("nobreakspace"))return 0x00a0;
        if(keyname.equals("exclamdown"))return 0x00a1;
        if(keyname.equals("cent"))return 0x00a2;
        if(keyname.equals("sterling"))return 0x00a3;
        if(keyname.equals("currency"))return 0x00a4;
        if(keyname.equals("yen"))return 0x00a5;
        if(keyname.equals("brokenbar"))return 0x00a6;
        if(keyname.equals("section"))return 0x00a7;
        if(keyname.equals("diaeresis"))return 0x00a8;
        if(keyname.equals("copyright"))return 0x00a9;
        if(keyname.equals("ordfeminine"))return 0x00aa;
        if(keyname.equals("guillemotleft"))return 0x00ab;
        if(keyname.equals("notsign"))return 0x00ac;
        if(keyname.equals("hyphen"))return 0x00ad;
        if(keyname.equals("registered"))return 0x00ae;
        if(keyname.equals("macron"))return 0x00af;
        if(keyname.equals("degree"))return 0x00b0;
        if(keyname.equals("plusminus"))return 0x00b1;
        if(keyname.equals("twosuperior"))return 0x00b2;
        if(keyname.equals("threesuperior"))return 0x00b3;
        if(keyname.equals("acute"))return 0x00b4;
        if(keyname.equals("mu"))return 0x00b5;
        if(keyname.equals("paragraph"))return 0x00b6;
        if(keyname.equals("periodcentered"))return 0x00b7;
        if(keyname.equals("cedilla"))return 0x00b8;
        if(keyname.equals("onesuperior"))return 0x00b9;
        if(keyname.equals("masculine"))return 0x00ba;
        if(keyname.equals("guillemotright"))return 0x00bb;
        if(keyname.equals("onequarter"))return 0x00bc;
        if(keyname.equals("onehalf"))return 0x00bd;
        if(keyname.equals("threequarters"))return 0x00be;
        if(keyname.equals("questiondown"))return 0x00bf;
        if(keyname.equals("Agrave"))return 0x00c0;
        if(keyname.equals("Aacute"))return 0x00c1;
        if(keyname.equals("Acircumflex"))return 0x00c2;
        if(keyname.equals("Atilde"))return 0x00c3;
        if(keyname.equals("Adiaeresis"))return 0x00c4;
        if(keyname.equals("Aring"))return 0x00c5;
        if(keyname.equals("AE"))return 0x00c6;
        if(keyname.equals("Ccedilla"))return 0x00c7;
        if(keyname.equals("Egrave"))return 0x00c8;
        if(keyname.equals("Eacute"))return 0x00c9;
        if(keyname.equals("Ecircumflex"))return 0x00ca;
        if(keyname.equals("Ediaeresis"))return 0x00cb;
        if(keyname.equals("Igrave"))return 0x00cc;
        if(keyname.equals("Iacute"))return 0x00cd;
        if(keyname.equals("Icircumflex"))return 0x00ce;
        if(keyname.equals("Idiaeresis"))return 0x00cf;
        if(keyname.equals("Eth"))return 0x00d0;
        if(keyname.equals("Ntilde"))return 0x00d1;
        if(keyname.equals("Ograve"))return 0x00d2;
        if(keyname.equals("Oacute"))return 0x00d3;
        if(keyname.equals("Ocircumflex"))return 0x00d4;
        if(keyname.equals("Otilde"))return 0x00d5;
        if(keyname.equals("Odiaeresis"))return 0x00d6;
        if(keyname.equals("multiply"))return 0x00d7;
        if(keyname.equals("Ooblique"))return 0x00d8;
        if(keyname.equals("Ugrave"))return 0x00d9;
        if(keyname.equals("Uacute"))return 0x00da;
        if(keyname.equals("Ucircumflex"))return 0x00db;
        if(keyname.equals("Udiaeresis"))return 0x00dc;
        if(keyname.equals("Yacute"))return 0x00dd;
        if(keyname.equals("Thorn"))return 0x00de;
        if(keyname.equals("ssharp"))return 0x00df;
        if(keyname.equals("agrave"))return 0x00e0;
        if(keyname.equals("aacute"))return 0x00e1;
        if(keyname.equals("acircumflex"))return 0x00e2;
        if(keyname.equals("atilde"))return 0x00e3;
        if(keyname.equals("adiaeresis"))return 0x00e4;
        if(keyname.equals("aring"))return 0x00e5;
        if(keyname.equals("ae"))return 0x00e6;
        if(keyname.equals("ccedilla"))return 0x00e7;
        if(keyname.equals("egrave"))return 0x00e8;
        if(keyname.equals("eacute"))return 0x00e9;
        if(keyname.equals("ecircumflex"))return 0x00ea;
        if(keyname.equals("ediaeresis"))return 0x00eb;
        if(keyname.equals("igrave"))return 0x00ec;
        if(keyname.equals("iacute"))return 0x00ed;
        if(keyname.equals("icircumflex"))return 0x00ee;
        if(keyname.equals("idiaeresis"))return 0x00ef;
        if(keyname.equals("eth"))return 0x00f0;
        if(keyname.equals("ntilde"))return 0x00f1;
        if(keyname.equals("ograve"))return 0x00f2;
        if(keyname.equals("oacute"))return 0x00f3;
        if(keyname.equals("ocircumflex"))return 0x00f4;
        if(keyname.equals("otilde"))return 0x00f5;
        if(keyname.equals("odiaeresis"))return 0x00f6;
        if(keyname.equals("division"))return 0x00f7;
        if(keyname.equals("oslash"))return 0x00f8;
        if(keyname.equals("ugrave"))return 0x00f9;
        if(keyname.equals("uacute"))return 0x00fa;
        if(keyname.equals("ucircumflex"))return 0x00fb;
        if(keyname.equals("udiaeresis"))return 0x00fc;
        if(keyname.equals("yacute"))return 0x00fd;
        if(keyname.equals("thorn"))return 0x00fe;
        if(keyname.equals("ydiaeresis"))return 0x00ff;
        if(keyname.equals("Aogonek"))return 0x01a1;
        if(keyname.equals("breve"))return 0x01a2;
        if(keyname.equals("Lstroke"))return 0x01a3;
        if(keyname.equals("Lcaron"))return 0x01a5;
        if(keyname.equals("Sacute"))return 0x01a6;
        if(keyname.equals("Scaron"))return 0x01a9;
        if(keyname.equals("Scedilla"))return 0x01aa;
        if(keyname.equals("Tcaron"))return 0x01ab;
        if(keyname.equals("Zacute"))return 0x01ac;
        if(keyname.equals("Zcaron"))return 0x01ae;
        if(keyname.equals("Zabovedot"))return 0x01af;
        if(keyname.equals("aogonek"))return 0x01b1;
        if(keyname.equals("ogonek"))return 0x01b2;
        if(keyname.equals("lstroke"))return 0x01b3;
        if(keyname.equals("lcaron"))return 0x01b5;
        if(keyname.equals("sacute"))return 0x01b6;
        if(keyname.equals("caron"))return 0x01b7;
        if(keyname.equals("scaron"))return 0x01b9;
        if(keyname.equals("scedilla"))return 0x01ba;
        if(keyname.equals("tcaron"))return 0x01bb;
        if(keyname.equals("zacute"))return 0x01bc;
        if(keyname.equals("doubleacute"))return 0x01bd;
        if(keyname.equals("zcaron"))return 0x01be;
        if(keyname.equals("zabovedot"))return 0x01bf;
        if(keyname.equals("Racute"))return 0x01c0;
        if(keyname.equals("Abreve"))return 0x01c3;
        if(keyname.equals("Cacute"))return 0x01c6;
        if(keyname.equals("Ccaron"))return 0x01c8;
        if(keyname.equals("Eogonek"))return 0x01ca;
        if(keyname.equals("Ecaron"))return 0x01cc;
        if(keyname.equals("Dcaron"))return 0x01cf;
        if(keyname.equals("Nacute"))return 0x01d1;
        if(keyname.equals("Ncaron"))return 0x01d2;
        if(keyname.equals("Odoubleacute"))return 0x01d5;
        if(keyname.equals("Rcaron"))return 0x01d8;
        if(keyname.equals("Uring"))return 0x01d9;
        if(keyname.equals("Udoubleacute"))return 0x01db;
        if(keyname.equals("Tcedilla"))return 0x01de;
        if(keyname.equals("racute"))return 0x01e0;
        if(keyname.equals("abreve"))return 0x01e3;
        if(keyname.equals("cacute"))return 0x01e6;
        if(keyname.equals("ccaron"))return 0x01e8;
        if(keyname.equals("eogonek"))return 0x01ea;
        if(keyname.equals("ecaron"))return 0x01ec;
        if(keyname.equals("dcaron"))return 0x01ef;
        if(keyname.equals("nacute"))return 0x01f1;
        if(keyname.equals("ncaron"))return 0x01f2;
        if(keyname.equals("odoubleacute"))return 0x01f5;
        if(keyname.equals("rcaron"))return 0x01f8;
        if(keyname.equals("uring"))return 0x01f9;
        if(keyname.equals("udoubleacute"))return 0x01fb;
        if(keyname.equals("tcedilla"))return 0x01fe;
        if(keyname.equals("abovedot"))return 0x01ff;
        if(keyname.equals("Hstroke"))return 0x02a1;
        if(keyname.equals("Hcircumflex"))return 0x02a6;
        if(keyname.equals("Iabovedot"))return 0x02a9;
        if(keyname.equals("Gbreve"))return 0x02ab;
        if(keyname.equals("Jcircumflex"))return 0x02ac;
        if(keyname.equals("hstroke"))return 0x02b1;
        if(keyname.equals("hcircumflex"))return 0x02b6;
        if(keyname.equals("idotless"))return 0x02b9;
        if(keyname.equals("gbreve"))return 0x02bb;
        if(keyname.equals("jcircumflex"))return 0x02bc;
        if(keyname.equals("Cabovedot"))return 0x02c5;
        if(keyname.equals("Ccircumflex"))return 0x02c6;
        if(keyname.equals("Gabovedot"))return 0x02d5;
        if(keyname.equals("Gcircumflex"))return 0x02d8;
        if(keyname.equals("Ubreve"))return 0x02dd;
        if(keyname.equals("Scircumflex"))return 0x02de;
        if(keyname.equals("cabovedot"))return 0x02e5;
        if(keyname.equals("ccircumflex"))return 0x02e6;
        if(keyname.equals("gabovedot"))return 0x02f5;
        if(keyname.equals("gcircumflex"))return 0x02f8;
        if(keyname.equals("ubreve"))return 0x02fd;
        if(keyname.equals("scircumflex"))return 0x02fe;
        if(keyname.equals("kappa"))return 0x03a2;
        if(keyname.equals("Rcedilla"))return 0x03a3;
        if(keyname.equals("Itilde"))return 0x03a5;
        if(keyname.equals("Lcedilla"))return 0x03a6;
        if(keyname.equals("Emacron"))return 0x03aa;
        if(keyname.equals("Gcedilla"))return 0x03ab;
        if(keyname.equals("Tslash"))return 0x03ac;
        if(keyname.equals("rcedilla"))return 0x03b3;
        if(keyname.equals("itilde"))return 0x03b5;
        if(keyname.equals("lcedilla"))return 0x03b6;
        if(keyname.equals("emacron"))return 0x03ba;
        if(keyname.equals("gacute"))return 0x03bb;
        if(keyname.equals("tslash"))return 0x03bc;
        if(keyname.equals("ENG"))return 0x03bd;
        if(keyname.equals("eng"))return 0x03bf;
        if(keyname.equals("Amacron"))return 0x03c0;
        if(keyname.equals("Iogonek"))return 0x03c7;
        if(keyname.equals("Eabovedot"))return 0x03cc;
        if(keyname.equals("Imacron"))return 0x03cf;
        if(keyname.equals("Ncedilla"))return 0x03d1;
        if(keyname.equals("Omacron"))return 0x03d2;
        if(keyname.equals("Kcedilla"))return 0x03d3;
        if(keyname.equals("Uogonek"))return 0x03d9;
        if(keyname.equals("Utilde"))return 0x03dd;
        if(keyname.equals("Umacron"))return 0x03de;
        if(keyname.equals("amacron"))return 0x03e0;
        if(keyname.equals("iogonek"))return 0x03e7;
        if(keyname.equals("eabovedot"))return 0x03ec;
        if(keyname.equals("imacron"))return 0x03ef;
        if(keyname.equals("ncedilla"))return 0x03f1;
        if(keyname.equals("omacron"))return 0x03f2;
        if(keyname.equals("kcedilla"))return 0x03f3;
        if(keyname.equals("uogonek"))return 0x03f9;
        if(keyname.equals("utilde"))return 0x03fd;
        if(keyname.equals("umacron"))return 0x03fe;
        if(keyname.equals("overline"))return 0x047e;
        if(keyname.equals("kana_fullstop"))return 0x04a1;
        if(keyname.equals("kana_openingbracket"))return 0x04a2;
        if(keyname.equals("kana_closingbracket"))return 0x04a3;
        if(keyname.equals("kana_comma"))return 0x04a4;
        if(keyname.equals("kana_middledot"))return 0x04a5;
        if(keyname.equals("kana_WO"))return 0x04a6;
        if(keyname.equals("kana_a"))return 0x04a7;
        if(keyname.equals("kana_i"))return 0x04a8;
        if(keyname.equals("kana_u"))return 0x04a9;
        if(keyname.equals("kana_e"))return 0x04aa;
        if(keyname.equals("kana_o"))return 0x04ab;
        if(keyname.equals("kana_ya"))return 0x04ac;
        if(keyname.equals("kana_yu"))return 0x04ad;
        if(keyname.equals("kana_yo"))return 0x04ae;
        if(keyname.equals("kana_tu"))return 0x04af;
        if(keyname.equals("prolongedsound"))return 0x04b0;
        if(keyname.equals("kana_A"))return 0x04b1;
        if(keyname.equals("kana_I"))return 0x04b2;
        if(keyname.equals("kana_U"))return 0x04b3;
        if(keyname.equals("kana_E"))return 0x04b4;
        if(keyname.equals("kana_O"))return 0x04b5;
        if(keyname.equals("kana_KA"))return 0x04b6;
        if(keyname.equals("kana_KI"))return 0x04b7;
        if(keyname.equals("kana_KU"))return 0x04b8;
        if(keyname.equals("kana_KE"))return 0x04b9;
        if(keyname.equals("kana_KO"))return 0x04ba;
        if(keyname.equals("kana_SA"))return 0x04bb;
        if(keyname.equals("kana_SHI"))return 0x04bc;
        if(keyname.equals("kana_SU"))return 0x04bd;
        if(keyname.equals("kana_SE"))return 0x04be;
        if(keyname.equals("kana_SO"))return 0x04bf;
        if(keyname.equals("kana_TA"))return 0x04c0;
        if(keyname.equals("kana_TI"))return 0x04c1;
        if(keyname.equals("kana_TU"))return 0x04c2;
        if(keyname.equals("kana_TE"))return 0x04c3;
        if(keyname.equals("kana_TO"))return 0x04c4;
        if(keyname.equals("kana_NA"))return 0x04c5;
        if(keyname.equals("kana_NI"))return 0x04c6;
        if(keyname.equals("kana_NU"))return 0x04c7;
        if(keyname.equals("kana_NE"))return 0x04c8;
        if(keyname.equals("kana_NO"))return 0x04c9;
        if(keyname.equals("kana_HA"))return 0x04ca;
        if(keyname.equals("kana_HI"))return 0x04cb;
        if(keyname.equals("kana_HU"))return 0x04cc;
        if(keyname.equals("kana_HE"))return 0x04cd;
        if(keyname.equals("kana_HO"))return 0x04ce;
        if(keyname.equals("kana_MA"))return 0x04cf;
        if(keyname.equals("kana_MI"))return 0x04d0;
        if(keyname.equals("kana_MU"))return 0x04d1;
        if(keyname.equals("kana_ME"))return 0x04d2;
        if(keyname.equals("kana_MO"))return 0x04d3;
        if(keyname.equals("kana_YA"))return 0x04d4;
        if(keyname.equals("kana_YU"))return 0x04d5;
        if(keyname.equals("kana_YO"))return 0x04d6;
        if(keyname.equals("kana_RA"))return 0x04d7;
        if(keyname.equals("kana_RI"))return 0x04d8;
        if(keyname.equals("kana_RU"))return 0x04d9;
        if(keyname.equals("kana_RE"))return 0x04da;
        if(keyname.equals("kana_RO"))return 0x04db;
        if(keyname.equals("kana_WA"))return 0x04dc;
        if(keyname.equals("kana_N"))return 0x04dd;
        if(keyname.equals("voicedsound"))return 0x04de;
        if(keyname.equals("semivoicedsound"))return 0x04df;
        if(keyname.equals("Arabic_comma"))return 0x05ac;
        if(keyname.equals("Arabic_semicolon"))return 0x05bb;
        if(keyname.equals("Arabic_question_mark"))return 0x05bf;
        if(keyname.equals("Arabic_hamza"))return 0x05c1;
        if(keyname.equals("Arabic_maddaonalef"))return 0x05c2;
        if(keyname.equals("Arabic_hamzaonalef"))return 0x05c3;
        if(keyname.equals("Arabic_hamzaonwaw"))return 0x05c4;
        if(keyname.equals("Arabic_hamzaunderalef"))return 0x05c5;
        if(keyname.equals("Arabic_hamzaonyeh"))return 0x05c6;
        if(keyname.equals("Arabic_alef"))return 0x05c7;
        if(keyname.equals("Arabic_beh"))return 0x05c8;
        if(keyname.equals("Arabic_tehmarbuta"))return 0x05c9;
        if(keyname.equals("Arabic_teh"))return 0x05ca;
        if(keyname.equals("Arabic_theh"))return 0x05cb;
        if(keyname.equals("Arabic_jeem"))return 0x05cc;
        if(keyname.equals("Arabic_hah"))return 0x05cd;
        if(keyname.equals("Arabic_khah"))return 0x05ce;
        if(keyname.equals("Arabic_dal"))return 0x05cf;
        if(keyname.equals("Arabic_thal"))return 0x05d0;
        if(keyname.equals("Arabic_ra"))return 0x05d1;
        if(keyname.equals("Arabic_zain"))return 0x05d2;
        if(keyname.equals("Arabic_seen"))return 0x05d3;
        if(keyname.equals("Arabic_sheen"))return 0x05d4;
        if(keyname.equals("Arabic_sad"))return 0x05d5;
        if(keyname.equals("Arabic_dad"))return 0x05d6;
        if(keyname.equals("Arabic_tah"))return 0x05d7;
        if(keyname.equals("Arabic_zah"))return 0x05d8;
        if(keyname.equals("Arabic_ain"))return 0x05d9;
        if(keyname.equals("Arabic_ghain"))return 0x05da;
        if(keyname.equals("Arabic_tatweel"))return 0x05e0;
        if(keyname.equals("Arabic_feh"))return 0x05e1;
        if(keyname.equals("Arabic_qaf"))return 0x05e2;
        if(keyname.equals("Arabic_kaf"))return 0x05e3;
        if(keyname.equals("Arabic_lam"))return 0x05e4;
        if(keyname.equals("Arabic_meem"))return 0x05e5;
        if(keyname.equals("Arabic_noon"))return 0x05e6;
        if(keyname.equals("Arabic_heh"))return 0x05e7;
        if(keyname.equals("Arabic_waw"))return 0x05e8;
        if(keyname.equals("Arabic_alefmaksura"))return 0x05e9;
        if(keyname.equals("Arabic_yeh"))return 0x05ea;
        if(keyname.equals("Arabic_fathatan"))return 0x05eb;
        if(keyname.equals("Arabic_dammatan"))return 0x05ec;
        if(keyname.equals("Arabic_kasratan"))return 0x05ed;
        if(keyname.equals("Arabic_fatha"))return 0x05ee;
        if(keyname.equals("Arabic_damma"))return 0x05ef;
        if(keyname.equals("Arabic_kasra"))return 0x05f0;
        if(keyname.equals("Arabic_shadda"))return 0x05f1;
        if(keyname.equals("Arabic_sukun"))return 0x05f2;
        if(keyname.equals("Serbian_dje"))return 0x06a1;
        if(keyname.equals("Macedonia_gje"))return 0x06a2;
        if(keyname.equals("Cyrillic_io"))return 0x06a3;
        if(keyname.equals("Ukranian_je"))return 0x06a4;
        if(keyname.equals("Macedonia_dse"))return 0x06a5;
        if(keyname.equals("Ukranian_i"))return 0x06a6;
        if(keyname.equals("Ukranian_yi"))return 0x06a7;
        if(keyname.equals("Serbian_je"))return 0x06a8;
        if(keyname.equals("Serbian_lje"))return 0x06a9;
        if(keyname.equals("Serbian_nje"))return 0x06aa;
        if(keyname.equals("Serbian_tshe"))return 0x06ab;
        if(keyname.equals("Macedonia_kje"))return 0x06ac;
        if(keyname.equals("Byelorussian_shortu"))return 0x06ae;
        if(keyname.equals("Serbian_dze"))return 0x06af;
        if(keyname.equals("numerosign"))return 0x06b0;
        if(keyname.equals("Serbian_DJE"))return 0x06b1;
        if(keyname.equals("Macedonia_GJE"))return 0x06b2;
        if(keyname.equals("Cyrillic_IO"))return 0x06b3;
        if(keyname.equals("Ukranian_JE"))return 0x06b4;
        if(keyname.equals("Macedonia_DSE"))return 0x06b5;
        if(keyname.equals("Ukranian_I"))return 0x06b6;
        if(keyname.equals("Ukranian_YI"))return 0x06b7;
        if(keyname.equals("Serbian_JE"))return 0x06b8;
        if(keyname.equals("Serbian_LJE"))return 0x06b9;
        if(keyname.equals("Serbian_NJE"))return 0x06ba;
        if(keyname.equals("Serbian_TSHE"))return 0x06bb;
        if(keyname.equals("Macedonia_KJE"))return 0x06bc;
        if(keyname.equals("Byelorussian_SHORTU"))return 0x06be;
        if(keyname.equals("Serbian_DZE"))return 0x06bf;
        if(keyname.equals("Cyrillic_yu"))return 0x06c0;
        if(keyname.equals("Cyrillic_a"))return 0x06c1;
        if(keyname.equals("Cyrillic_be"))return 0x06c2;
        if(keyname.equals("Cyrillic_tse"))return 0x06c3;
        if(keyname.equals("Cyrillic_de"))return 0x06c4;
        if(keyname.equals("Cyrillic_ie"))return 0x06c5;
        if(keyname.equals("Cyrillic_ef"))return 0x06c6;
        if(keyname.equals("Cyrillic_ghe"))return 0x06c7;
        if(keyname.equals("Cyrillic_ha"))return 0x06c8;
        if(keyname.equals("Cyrillic_i"))return 0x06c9;
        if(keyname.equals("Cyrillic_shorti"))return 0x06ca;
        if(keyname.equals("Cyrillic_ka"))return 0x06cb;
        if(keyname.equals("Cyrillic_el"))return 0x06cc;
        if(keyname.equals("Cyrillic_em"))return 0x06cd;
        if(keyname.equals("Cyrillic_en"))return 0x06ce;
        if(keyname.equals("Cyrillic_o"))return 0x06cf;
        if(keyname.equals("Cyrillic_pe"))return 0x06d0;
        if(keyname.equals("Cyrillic_ya"))return 0x06d1;
        if(keyname.equals("Cyrillic_er"))return 0x06d2;
        if(keyname.equals("Cyrillic_es"))return 0x06d3;
        if(keyname.equals("Cyrillic_te"))return 0x06d4;
        if(keyname.equals("Cyrillic_u"))return 0x06d5;
        if(keyname.equals("Cyrillic_zhe"))return 0x06d6;
        if(keyname.equals("Cyrillic_ve"))return 0x06d7;
        if(keyname.equals("Cyrillic_softsign"))return 0x06d8;
        if(keyname.equals("Cyrillic_yeru"))return 0x06d9;
        if(keyname.equals("Cyrillic_ze"))return 0x06da;
        if(keyname.equals("Cyrillic_sha"))return 0x06db;
        if(keyname.equals("Cyrillic_e"))return 0x06dc;
        if(keyname.equals("Cyrillic_shcha"))return 0x06dd;
        if(keyname.equals("Cyrillic_che"))return 0x06de;
        if(keyname.equals("Cyrillic_hardsign"))return 0x06df;
        if(keyname.equals("Cyrillic_YU"))return 0x06e0;
        if(keyname.equals("Cyrillic_A"))return 0x06e1;
        if(keyname.equals("Cyrillic_BE"))return 0x06e2;
        if(keyname.equals("Cyrillic_TSE"))return 0x06e3;
        if(keyname.equals("Cyrillic_DE"))return 0x06e4;
        if(keyname.equals("Cyrillic_IE"))return 0x06e5;
        if(keyname.equals("Cyrillic_EF"))return 0x06e6;
        if(keyname.equals("Cyrillic_GHE"))return 0x06e7;
        if(keyname.equals("Cyrillic_HA"))return 0x06e8;
        if(keyname.equals("Cyrillic_I"))return 0x06e9;
        if(keyname.equals("Cyrillic_SHORTI"))return 0x06ea;
        if(keyname.equals("Cyrillic_KA"))return 0x06eb;
        if(keyname.equals("Cyrillic_EL"))return 0x06ec;
        if(keyname.equals("Cyrillic_EM"))return 0x06ed;
        if(keyname.equals("Cyrillic_EN"))return 0x06ee;
        if(keyname.equals("Cyrillic_O"))return 0x06ef;
        if(keyname.equals("Cyrillic_PE"))return 0x06f0;
        if(keyname.equals("Cyrillic_YA"))return 0x06f1;
        if(keyname.equals("Cyrillic_ER"))return 0x06f2;
        if(keyname.equals("Cyrillic_ES"))return 0x06f3;
        if(keyname.equals("Cyrillic_TE"))return 0x06f4;
        if(keyname.equals("Cyrillic_U"))return 0x06f5;
        if(keyname.equals("Cyrillic_ZHE"))return 0x06f6;
        if(keyname.equals("Cyrillic_VE"))return 0x06f7;
        if(keyname.equals("Cyrillic_SOFTSIGN"))return 0x06f8;
        if(keyname.equals("Cyrillic_YERU"))return 0x06f9;
        if(keyname.equals("Cyrillic_ZE"))return 0x06fa;
        if(keyname.equals("Cyrillic_SHA"))return 0x06fb;
        if(keyname.equals("Cyrillic_E"))return 0x06fc;
        if(keyname.equals("Cyrillic_SHCHA"))return 0x06fd;
        if(keyname.equals("Cyrillic_CHE"))return 0x06fe;
        if(keyname.equals("Cyrillic_HARDSIGN"))return 0x06ff;
        if(keyname.equals("Greek_ALPHAaccent"))return 0x07a1;
        if(keyname.equals("Greek_EPSILONaccent"))return 0x07a2;
        if(keyname.equals("Greek_ETAaccent"))return 0x07a3;
        if(keyname.equals("Greek_IOTAaccent"))return 0x07a4;
        if(keyname.equals("Greek_IOTAdiaeresis"))return 0x07a5;
        if(keyname.equals("Greek_IOTAaccentdiaeresis"))return 0x07a6;
        if(keyname.equals("Greek_OMICRONaccent"))return 0x07a7;
        if(keyname.equals("Greek_UPSILONaccent"))return 0x07a8;
        if(keyname.equals("Greek_UPSILONdieresis"))return 0x07a9;
        if(keyname.equals("Greek_UPSILONaccentdieresis"))return 0x07aa;
        if(keyname.equals("Greek_OMEGAaccent"))return 0x07ab;
        if(keyname.equals("Greek_alphaaccent"))return 0x07b1;
        if(keyname.equals("Greek_epsilonaccent"))return 0x07b2;
        if(keyname.equals("Greek_etaaccent"))return 0x07b3;
        if(keyname.equals("Greek_iotaaccent"))return 0x07b4;
        if(keyname.equals("Greek_iotadieresis"))return 0x07b5;
        if(keyname.equals("Greek_iotaaccentdieresis"))return 0x07b6;
        if(keyname.equals("Greek_omicronaccent"))return 0x07b7;
        if(keyname.equals("Greek_upsilonaccent"))return 0x07b8;
        if(keyname.equals("Greek_upsilondieresis"))return 0x07b9;
        if(keyname.equals("Greek_upsilonaccentdieresis"))return 0x07ba;
        if(keyname.equals("Greek_omegaaccent"))return 0x07bb;
        if(keyname.equals("Greek_ALPHA"))return 0x07c1;
        if(keyname.equals("Greek_BETA"))return 0x07c2;
        if(keyname.equals("Greek_GAMMA"))return 0x07c3;
        if(keyname.equals("Greek_DELTA"))return 0x07c4;
        if(keyname.equals("Greek_EPSILON"))return 0x07c5;
        if(keyname.equals("Greek_ZETA"))return 0x07c6;
        if(keyname.equals("Greek_ETA"))return 0x07c7;
        if(keyname.equals("Greek_THETA"))return 0x07c8;
        if(keyname.equals("Greek_IOTA"))return 0x07c9;
        if(keyname.equals("Greek_KAPPA"))return 0x07ca;
        if(keyname.equals("Greek_LAMBDA"))return 0x07cb;
        if(keyname.equals("Greek_MU"))return 0x07cc;
        if(keyname.equals("Greek_NU"))return 0x07cd;
        if(keyname.equals("Greek_XI"))return 0x07ce;
        if(keyname.equals("Greek_OMICRON"))return 0x07cf;
        if(keyname.equals("Greek_PI"))return 0x07d0;
        if(keyname.equals("Greek_RHO"))return 0x07d1;
        if(keyname.equals("Greek_SIGMA"))return 0x07d2;
        if(keyname.equals("Greek_TAU"))return 0x07d4;
        if(keyname.equals("Greek_UPSILON"))return 0x07d5;
        if(keyname.equals("Greek_PHI"))return 0x07d6;
        if(keyname.equals("Greek_CHI"))return 0x07d7;
        if(keyname.equals("Greek_PSI"))return 0x07d8;
        if(keyname.equals("Greek_OMEGA"))return 0x07d9;
        if(keyname.equals("Greek_alpha"))return 0x07e1;
        if(keyname.equals("Greek_beta"))return 0x07e2;
        if(keyname.equals("Greek_gamma"))return 0x07e3;
        if(keyname.equals("Greek_delta"))return 0x07e4;
        if(keyname.equals("Greek_epsilon"))return 0x07e5;
        if(keyname.equals("Greek_zeta"))return 0x07e6;
        if(keyname.equals("Greek_eta"))return 0x07e7;
        if(keyname.equals("Greek_theta"))return 0x07e8;
        if(keyname.equals("Greek_iota"))return 0x07e9;
        if(keyname.equals("Greek_kappa"))return 0x07ea;
        if(keyname.equals("Greek_lambda"))return 0x07eb;
        if(keyname.equals("Greek_mu"))return 0x07ec;
        if(keyname.equals("Greek_nu"))return 0x07ed;
        if(keyname.equals("Greek_xi"))return 0x07ee;
        if(keyname.equals("Greek_omicron"))return 0x07ef;
        if(keyname.equals("Greek_pi"))return 0x07f0;
        if(keyname.equals("Greek_rho"))return 0x07f1;
        if(keyname.equals("Greek_sigma"))return 0x07f2;
        if(keyname.equals("Greek_finalsmallsigma"))return 0x07f3;
        if(keyname.equals("Greek_tau"))return 0x07f4;
        if(keyname.equals("Greek_upsilon"))return 0x07f5;
        if(keyname.equals("Greek_phi"))return 0x07f6;
        if(keyname.equals("Greek_chi"))return 0x07f7;
        if(keyname.equals("Greek_psi"))return 0x07f8;
        if(keyname.equals("Greek_omega"))return 0x07f9;
        if(keyname.equals("leftradical"))return 0x08a1;
        if(keyname.equals("topleftradical"))return 0x08a2;
        if(keyname.equals("horizconnector"))return 0x08a3;
        if(keyname.equals("topintegral"))return 0x08a4;
        if(keyname.equals("botintegral"))return 0x08a5;
        if(keyname.equals("vertconnector"))return 0x08a6;
        if(keyname.equals("topleftsqbracket"))return 0x08a7;
        if(keyname.equals("botleftsqbracket"))return 0x08a8;
        if(keyname.equals("toprightsqbracket"))return 0x08a9;
        if(keyname.equals("botrightsqbracket"))return 0x08aa;
        if(keyname.equals("topleftparens"))return 0x08ab;
        if(keyname.equals("botleftparens"))return 0x08ac;
        if(keyname.equals("toprightparens"))return 0x08ad;
        if(keyname.equals("botrightparens"))return 0x08ae;
        if(keyname.equals("leftmiddlecurlybrace"))return 0x08af;
        if(keyname.equals("rightmiddlecurlybrace"))return 0x08b0;
        if(keyname.equals("topleftsummation"))return 0x08b1;
        if(keyname.equals("botleftsummation"))return 0x08b2;
        if(keyname.equals("topvertsummationconnector"))return 0x08b3;
        if(keyname.equals("botvertsummationconnector"))return 0x08b4;
        if(keyname.equals("toprightsummation"))return 0x08b5;
        if(keyname.equals("botrightsummation"))return 0x08b6;
        if(keyname.equals("rightmiddlesummation"))return 0x08b7;
        if(keyname.equals("lessthanequal"))return 0x08bc;
        if(keyname.equals("notequal"))return 0x08bd;
        if(keyname.equals("greaterthanequal"))return 0x08be;
        if(keyname.equals("integral"))return 0x08bf;
        if(keyname.equals("therefore"))return 0x08c0;
        if(keyname.equals("variation"))return 0x08c1;
        if(keyname.equals("infinity"))return 0x08c2;
        if(keyname.equals("nabla"))return 0x08c5;
        if(keyname.equals("approximate"))return 0x08c8;
        if(keyname.equals("similarequal"))return 0x08c9;
        if(keyname.equals("ifonlyif"))return 0x08cd;
        if(keyname.equals("implies"))return 0x08ce;
        if(keyname.equals("identical"))return 0x08cf;
        if(keyname.equals("radical"))return 0x08d6;
        if(keyname.equals("includedin"))return 0x08da;
        if(keyname.equals("includes"))return 0x08db;
        if(keyname.equals("intersection"))return 0x08dc;
        if(keyname.equals("union"))return 0x08dd;
        if(keyname.equals("logicaland"))return 0x08de;
        if(keyname.equals("logicalor"))return 0x08df;
        if(keyname.equals("partialderivative"))return 0x08ef;
        if(keyname.equals("function"))return 0x08f6;
        if(keyname.equals("leftarrow"))return 0x08fb;
        if(keyname.equals("uparrow"))return 0x08fc;
        if(keyname.equals("rightarrow"))return 0x08fd;
        if(keyname.equals("downarrow"))return 0x08fe;
        if(keyname.equals("blank"))return 0x09df;
        if(keyname.equals("soliddiamond"))return 0x09e0;
        if(keyname.equals("checkerboard"))return 0x09e1;
        if(keyname.equals("ht"))return 0x09e2;
        if(keyname.equals("ff"))return 0x09e3;
        if(keyname.equals("cr"))return 0x09e4;
        if(keyname.equals("lf"))return 0x09e5;
        if(keyname.equals("nl"))return 0x09e8;
        if(keyname.equals("vt"))return 0x09e9;
        if(keyname.equals("lowrightcorner"))return 0x09ea;
        if(keyname.equals("uprightcorner"))return 0x09eb;
        if(keyname.equals("upleftcorner"))return 0x09ec;
        if(keyname.equals("lowleftcorner"))return 0x09ed;
        if(keyname.equals("crossinglines"))return 0x09ee;
        if(keyname.equals("horizlinescan1"))return 0x09ef;
        if(keyname.equals("horizlinescan3"))return 0x09f0;
        if(keyname.equals("horizlinescan5"))return 0x09f1;
        if(keyname.equals("horizlinescan7"))return 0x09f2;
        if(keyname.equals("horizlinescan9"))return 0x09f3;
        if(keyname.equals("leftt"))return 0x09f4;
        if(keyname.equals("rightt"))return 0x09f5;
        if(keyname.equals("bott"))return 0x09f6;
        if(keyname.equals("topt"))return 0x09f7;
        if(keyname.equals("vertbar"))return 0x09f8;
        if(keyname.equals("emspace"))return 0x0aa1;
        if(keyname.equals("enspace"))return 0x0aa2;
        if(keyname.equals("em3space"))return 0x0aa3;
        if(keyname.equals("em4space"))return 0x0aa4;
        if(keyname.equals("digitspace"))return 0x0aa5;
        if(keyname.equals("punctspace"))return 0x0aa6;
        if(keyname.equals("thinspace"))return 0x0aa7;
        if(keyname.equals("hairspace"))return 0x0aa8;
        if(keyname.equals("emdash"))return 0x0aa9;
        if(keyname.equals("endash"))return 0x0aaa;
        if(keyname.equals("signifblank"))return 0x0aac;
        if(keyname.equals("ellipsis"))return 0x0aae;
        if(keyname.equals("doubbaselinedot"))return 0x0aaf;
        if(keyname.equals("onethird"))return 0x0ab0;
        if(keyname.equals("twothirds"))return 0x0ab1;
        if(keyname.equals("onefifth"))return 0x0ab2;
        if(keyname.equals("twofifths"))return 0x0ab3;
        if(keyname.equals("threefifths"))return 0x0ab4;
        if(keyname.equals("fourfifths"))return 0x0ab5;
        if(keyname.equals("onesixth"))return 0x0ab6;
        if(keyname.equals("fivesixths"))return 0x0ab7;
        if(keyname.equals("careof"))return 0x0ab8;
        if(keyname.equals("figdash"))return 0x0abb;
        if(keyname.equals("leftanglebracket"))return 0x0abc;
        if(keyname.equals("decimalpoint"))return 0x0abd;
        if(keyname.equals("rightanglebracket"))return 0x0abe;
        if(keyname.equals("marker"))return 0x0abf;
        if(keyname.equals("oneeighth"))return 0x0ac3;
        if(keyname.equals("threeeighths"))return 0x0ac4;
        if(keyname.equals("fiveeighths"))return 0x0ac5;
        if(keyname.equals("seveneighths"))return 0x0ac6;
        if(keyname.equals("trademark"))return 0x0ac9;
        if(keyname.equals("signaturemark"))return 0x0aca;
        if(keyname.equals("trademarkincircle"))return 0x0acb;
        if(keyname.equals("leftopentriangle"))return 0x0acc;
        if(keyname.equals("rightopentriangle"))return 0x0acd;
        if(keyname.equals("emopencircle"))return 0x0ace;
        if(keyname.equals("emopenrectangle"))return 0x0acf;
        if(keyname.equals("leftsinglequotemark"))return 0x0ad0;
        if(keyname.equals("rightsinglequotemark"))return 0x0ad1;
        if(keyname.equals("leftdoublequotemark"))return 0x0ad2;
        if(keyname.equals("rightdoublequotemark"))return 0x0ad3;
        if(keyname.equals("prescription"))return 0x0ad4;
        if(keyname.equals("minutes"))return 0x0ad6;
        if(keyname.equals("seconds"))return 0x0ad7;
        if(keyname.equals("latincross"))return 0x0ad9;
        if(keyname.equals("hexagram"))return 0x0ada;
        if(keyname.equals("filledrectbullet"))return 0x0adb;
        if(keyname.equals("filledlefttribullet"))return 0x0adc;
        if(keyname.equals("filledrighttribullet"))return 0x0add;
        if(keyname.equals("emfilledcircle"))return 0x0ade;
        if(keyname.equals("emfilledrect"))return 0x0adf;
        if(keyname.equals("enopencircbullet"))return 0x0ae0;
        if(keyname.equals("enopensquarebullet"))return 0x0ae1;
        if(keyname.equals("openrectbullet"))return 0x0ae2;
        if(keyname.equals("opentribulletup"))return 0x0ae3;
        if(keyname.equals("opentribulletdown"))return 0x0ae4;
        if(keyname.equals("openstar"))return 0x0ae5;
        if(keyname.equals("enfilledcircbullet"))return 0x0ae6;
        if(keyname.equals("enfilledsqbullet"))return 0x0ae7;
        if(keyname.equals("filledtribulletup"))return 0x0ae8;
        if(keyname.equals("filledtribulletdown"))return 0x0ae9;
        if(keyname.equals("leftpointer"))return 0x0aea;
        if(keyname.equals("rightpointer"))return 0x0aeb;
        if(keyname.equals("club"))return 0x0aec;
        if(keyname.equals("diamond"))return 0x0aed;
        if(keyname.equals("heart"))return 0x0aee;
        if(keyname.equals("maltesecross"))return 0x0af0;
        if(keyname.equals("dagger"))return 0x0af1;
        if(keyname.equals("doubledagger"))return 0x0af2;
        if(keyname.equals("checkmark"))return 0x0af3;
        if(keyname.equals("ballotcross"))return 0x0af4;
        if(keyname.equals("musicalsharp"))return 0x0af5;
        if(keyname.equals("musicalflat"))return 0x0af6;
        if(keyname.equals("malesymbol"))return 0x0af7;
        if(keyname.equals("femalesymbol"))return 0x0af8;
        if(keyname.equals("telephone"))return 0x0af9;
        if(keyname.equals("telephonerecorder"))return 0x0afa;
        if(keyname.equals("phonographcopyright"))return 0x0afb;
        if(keyname.equals("caret"))return 0x0afc;
        if(keyname.equals("singlelowquotemark"))return 0x0afd;
        if(keyname.equals("doublelowquotemark"))return 0x0afe;
        if(keyname.equals("cursor"))return 0x0aff;
        if(keyname.equals("leftcaret"))return 0x0ba3;
        if(keyname.equals("rightcaret"))return 0x0ba6;
        if(keyname.equals("downcaret"))return 0x0ba8;
        if(keyname.equals("upcaret"))return 0x0ba9;
        if(keyname.equals("overbar"))return 0x0bc0;
        if(keyname.equals("downtack"))return 0x0bc2;
        if(keyname.equals("upshoe"))return 0x0bc3;
        if(keyname.equals("downstile"))return 0x0bc4;
        if(keyname.equals("underbar"))return 0x0bc6;
        if(keyname.equals("jot"))return 0x0bca;
        if(keyname.equals("quad"))return 0x0bcc;
        if(keyname.equals("uptack"))return 0x0bce;
        if(keyname.equals("circle"))return 0x0bcf;
        if(keyname.equals("upstile"))return 0x0bd3;
        if(keyname.equals("downshoe"))return 0x0bd6;
        if(keyname.equals("rightshoe"))return 0x0bd8;
        if(keyname.equals("leftshoe"))return 0x0bda;
        if(keyname.equals("lefttack"))return 0x0bdc;
        if(keyname.equals("righttack"))return 0x0bfc;
        if(keyname.equals("hebrew_aleph"))return 0x0ce0;
        if(keyname.equals("hebrew_beth"))return 0x0ce1;
        if(keyname.equals("hebrew_gimmel"))return 0x0ce2;
        if(keyname.equals("hebrew_daleth"))return 0x0ce3;
        if(keyname.equals("hebrew_he"))return 0x0ce4;
        if(keyname.equals("hebrew_waw"))return 0x0ce5;
        if(keyname.equals("hebrew_zayin"))return 0x0ce6;
        if(keyname.equals("hebrew_het"))return 0x0ce7;
        if(keyname.equals("hebrew_teth"))return 0x0ce8;
        if(keyname.equals("hebrew_yod"))return 0x0ce9;
        if(keyname.equals("hebrew_finalkaph"))return 0x0cea;
        if(keyname.equals("hebrew_kaph"))return 0x0ceb;
        if(keyname.equals("hebrew_lamed"))return 0x0cec;
        if(keyname.equals("hebrew_finalmem"))return 0x0ced;
        if(keyname.equals("hebrew_mem"))return 0x0cee;
        if(keyname.equals("hebrew_finalnun"))return 0x0cef;
        if(keyname.equals("hebrew_nun"))return 0x0cf0;
        if(keyname.equals("hebrew_samekh"))return 0x0cf1;
        if(keyname.equals("hebrew_ayin"))return 0x0cf2;
        if(keyname.equals("hebrew_finalpe"))return 0x0cf3;
        if(keyname.equals("hebrew_pe"))return 0x0cf4;
        if(keyname.equals("hebrew_finalzadi"))return 0x0cf5;
        if(keyname.equals("hebrew_zadi"))return 0x0cf6;
        if(keyname.equals("hebrew_kuf"))return 0x0cf7;
        if(keyname.equals("hebrew_resh"))return 0x0cf8;
        if(keyname.equals("hebrew_shin"))return 0x0cf9;
        if(keyname.equals("hebrew_taf"))return 0x0cfa;
        if(keyname.equals("BackSpace"))return 0xff08;
        if(keyname.equals("Tab"))return 0xff09;
        if(keyname.equals("Linefeed"))return 0xff0a;
        if(keyname.equals("Clear"))return 0xff0b;
        if(keyname.equals("Return"))return 0xff0d;
        if(keyname.equals("Pause"))return 0xff13;
        if(keyname.equals("Scroll_Lock"))return 0xff14;
        if(keyname.equals("Sys_Req"))return 0xff15;
        if(keyname.equals("Escape"))return 0xff1b;
        if(keyname.equals("Multi_key"))return 0xff20;
        if(keyname.equals("Kanji"))return 0xff21;
        if(keyname.equals("Home"))return 0xff50;
        if(keyname.equals("Left"))return 0xff51;
        if(keyname.equals("Up"))return 0xff52;
        if(keyname.equals("Right"))return 0xff53;
        if(keyname.equals("Down"))return 0xff54;
        if(keyname.equals("Prior"))return 0xff55;
        if(keyname.equals("Next"))return 0xff56;
        if(keyname.equals("End"))return 0xff57;
        if(keyname.equals("Begin"))return 0xff58;
        if(keyname.equals("Win_L"))return 0xff5b;
        if(keyname.equals("Win_R"))return 0xff5c;
        if(keyname.equals("App"))return 0xff5d;
        if(keyname.equals("Select"))return 0xff60;
        if(keyname.equals("Print"))return 0xff61;
        if(keyname.equals("Execute"))return 0xff62;
        if(keyname.equals("Insert"))return 0xff63;
        if(keyname.equals("Undo"))return 0xff65;
        if(keyname.equals("Redo"))return 0xff66;
        if(keyname.equals("Menu"))return 0xff67;
        if(keyname.equals("Find"))return 0xff68;
        if(keyname.equals("Cancel"))return 0xff69;
        if(keyname.equals("Help"))return 0xff6a;
        if(keyname.equals("Break"))return 0xff6b;
        if(keyname.equals("Hebrew_switch"))return 0xff7e;
        if(keyname.equals("Num_Lock"))return 0xff7f;
        if(keyname.equals("KP_Space"))return 0xff80;
        if(keyname.equals("KP_Tab"))return 0xff89;
        if(keyname.equals("KP_Enter"))return 0xff8d;
        if(keyname.equals("KP_F1"))return 0xff91;
        if(keyname.equals("KP_F2"))return 0xff92;
        if(keyname.equals("KP_F3"))return 0xff93;
        if(keyname.equals("KP_F4"))return 0xff94;
        if(keyname.equals("KP_Multiply"))return 0xffaa;
        if(keyname.equals("KP_Add"))return 0xffab;
        if(keyname.equals("KP_Separator"))return 0xffac;
        if(keyname.equals("KP_Subtract"))return 0xffad;
        if(keyname.equals("KP_Decimal"))return 0xffae;
        if(keyname.equals("KP_Divide"))return 0xffaf;
        if(keyname.equals("KP_0"))return 0xffb0;
        if(keyname.equals("KP_1"))return 0xffb1;
        if(keyname.equals("KP_2"))return 0xffb2;
        if(keyname.equals("KP_3"))return 0xffb3;
        if(keyname.equals("KP_4"))return 0xffb4;
        if(keyname.equals("KP_5"))return 0xffb5;
        if(keyname.equals("KP_6"))return 0xffb6;
        if(keyname.equals("KP_7"))return 0xffb7;
        if(keyname.equals("KP_8"))return 0xffb8;
        if(keyname.equals("KP_9"))return 0xffb9;
        if(keyname.equals("KP_Equal"))return 0xffbd;
        if(keyname.equals("F1"))return 0xffbe;
        if(keyname.equals("F2"))return 0xffbf;
        if(keyname.equals("F3"))return 0xffc0;
        if(keyname.equals("F4"))return 0xffc1;
        if(keyname.equals("F5"))return 0xffc2;
        if(keyname.equals("F6"))return 0xffc3;
        if(keyname.equals("F7"))return 0xffc4;
        if(keyname.equals("F8"))return 0xffc5;
        if(keyname.equals("F9"))return 0xffc6;
        if(keyname.equals("F10"))return 0xffc7;
        if(keyname.equals("L1"))return 0xffc8;
        if(keyname.equals("L2"))return 0xffc9;
        if(keyname.equals("L3"))return 0xffca;
        if(keyname.equals("L4"))return 0xffcb;
        if(keyname.equals("L5"))return 0xffcc;
        if(keyname.equals("L6"))return 0xffcd;
        if(keyname.equals("L7"))return 0xffce;
        if(keyname.equals("L8"))return 0xffcf;
        if(keyname.equals("L9"))return 0xffd0;
        if(keyname.equals("L10"))return 0xffd1;
        if(keyname.equals("R1"))return 0xffd2;
        if(keyname.equals("R2"))return 0xffd3;
        if(keyname.equals("R3"))return 0xffd4;
        if(keyname.equals("R4"))return 0xffd5;
        if(keyname.equals("R5"))return 0xffd6;
        if(keyname.equals("R6"))return 0xffd7;
        if(keyname.equals("R7"))return 0xffd8;
        if(keyname.equals("R8"))return 0xffd9;
        if(keyname.equals("R9"))return 0xffda;
        if(keyname.equals("R10"))return 0xffdb;
        if(keyname.equals("R11"))return 0xffdc;
        if(keyname.equals("R12"))return 0xffdd;
        if(keyname.equals("F33"))return 0xffde;
        if(keyname.equals("R14"))return 0xffdf;
        if(keyname.equals("R15"))return 0xffe0;
        if(keyname.equals("Shift_L"))return 0xffe1;
        if(keyname.equals("Shift_R"))return 0xffe2;
        if(keyname.equals("Control_L"))return 0xffe3;
        if(keyname.equals("Control_R"))return 0xffe4;
        if(keyname.equals("Caps_Lock"))return 0xffe5;
        if(keyname.equals("Shift_Lock"))return 0xffe6;
        if(keyname.equals("Meta_L"))return 0xffe7;
        if(keyname.equals("Meta_R"))return 0xffe8;
        if(keyname.equals("Alt_L"))return 0xffe9;
        if(keyname.equals("Alt_R"))return 0xffea;
        if(keyname.equals("Super_L"))return 0xffeb;
        if(keyname.equals("Super_R"))return 0xffec;
        if(keyname.equals("Hyper_L"))return 0xffed;
        if(keyname.equals("Hyper_R"))return 0xffee;
        if(keyname.equals("Delete"))return 0xffff;
        return -1;
    }

    public void pressKey(String key){
        int keynum=keyNameToKeySym(key);
        if(keynum==-1)return;
        client.type(keynum);
    }

    public void pressKey(String key,boolean state){
        int keynum=keyNameToKeySym(key);
        if(keynum==-1)return;
        client.updateKey(keynum,state);
    }

    public void holdKey(String key,int time){
        int keynum=keyNameToKeySym(key);
        if(keynum==-1)return;
        client.updateKey(keynum,true);
        (new Thread(()->{
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {}
            client.updateKey(keynum,false);
        })).start();
    }

    HashMap<Integer,Thread> keysHeld=new HashMap<>();

    public void holdKey(String key){
        int keynum=keyNameToKeySym(key);
        if(keynum==-1)return;
        if(keysHeld.containsKey(keynum)){
            ((Thread)keysHeld.get(keynum)).stop();
        }else{
            client.updateKey(keynum,true);
        }
        Thread thr=new Thread(()->{
            try {
                Thread.sleep(100);//2 ticks to be safe
            } catch (InterruptedException e) {}
            client.updateKey(keynum,false);
            keysHeld.remove(keynum);
        });
        thr.start();
        keysHeld.put(keynum,thr);
    }

    public void typeText(String text){
        client.type(text);
    }
}

public class VideoCapture extends Thread {
    public int width;
    public int height;
    MakiDesktop plugin;
    public static BufferedImage currentFrame;

    VideoCaptureVnc videoCaptureVnc;

    public VideoCapture(MakiDesktop plugin, int width, int height) {
        this.plugin = plugin;
        this.width = width;
        this.height = height;


        videoCaptureVnc = new VideoCaptureVnc() {
            @Override
            public void onFrame(BufferedImage frame) {
                currentFrame = frame;
            }
        };
        videoCaptureVnc.start();

    }

    public void cleanup() {
        videoCaptureVnc.cleanup();
    }

    public void clickMouse(double x,double y,int doClick,boolean drag){
        videoCaptureVnc.clickMouse(x,y,doClick,drag);
    }

    public void pressKey(String key){
        videoCaptureVnc.pressKey(key);
    }

    public void pressKey(String key,boolean state){
        videoCaptureVnc.pressKey(key,state);
    }

    public void holdKey(String key,int time){
        videoCaptureVnc.holdKey(key,time);
    }

    public void holdKey(String key){
        videoCaptureVnc.holdKey(key);
    }

    public void typeText(String text){
        videoCaptureVnc.typeText(text);
    }
}
