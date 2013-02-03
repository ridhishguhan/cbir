package com.mmir.cbir.engine;

import java.io.*;
 
public class JPEGDecoder {
    private int height;
 
    // Private variables and constants
    private static final int MSB = 0x80000000;
    private static final int MAX_HUFFMAN_SUBTREE = 50; // max size = MAX_HUFFMAN_SUBTREE * 256
    private int nComp; //number of Components in a scan
    private int[] qTab[] = new int[10][]; //quantization table for the i-th Comp in a scan
    private int[] dcTab[] = new int[10][]; //dc HuffTab for the i-th Comp in a scan
    private int[] acTab[] = new int[10][]; //ac HuffTab for the i-th Comp in a scan
    private int nBlock[] = new int[10]; //number of blocks in the i-th Comp in a scan
    //                  i=0, ... ,Ns-1
    private int YH, YV, Xsize, Ysize;
    private int marker;
    private int marker_index = 0;
    private int Ri = 0; // RestartInterval
 
    private int DU[][][] = new int[10][4][64]; //at most 10 data units in a MCU
    //at most 4 data units in one component
 
    private int x = 0, y = 0, num = 0, yp = 0; // the begin point of MCU
 
    private int IDCT_Source[] = new int[64];
    private final static int IDCT_P[] = {
                                        0, 5, 40, 16, 45, 2, 7, 42,
                                        21, 56, 8, 61, 18, 47, 1, 4,
                                        41, 23, 58, 13, 32, 24, 37, 10,
                                        63, 17, 44, 3, 6, 43, 20, 57,
                                        15, 34, 29, 48, 53, 26, 39, 9,
                                        60, 19, 46, 22, 59, 12, 33, 31,
                                        50, 55, 25, 36, 11, 62, 14, 35,
                                        28, 49, 52, 27, 38, 30, 51, 54
    };
    private final static int table[] = {
                                       0, 1, 5, 6, 14, 15, 27, 28,
                                       2, 4, 7, 13, 16, 26, 29, 42,
                                       3, 8, 12, 17, 25, 30, 41, 43,
                                       9, 11, 18, 24, 31, 40, 44, 53,
                                       10, 19, 23, 32, 39, 45, 52, 54,
                                       20, 22, 33, 38, 46, 51, 55, 60,
                                       21, 34, 37, 47, 50, 56, 59, 61,
                                       35, 36, 48, 49, 57, 58, 62, 63
    };
 
 
    private FrameHeader FH = new FrameHeader();
    private ScanHeader SH = new ScanHeader();
    private QuantizationTable QT = new QuantizationTable();
    private HuffmanTable HT = new HuffmanTable();
 
    private void error(String message) throws Exception {
        throw new Exception(message);
    }
 
    interface DCTArray {
        
        public void setSize(int width, int height);

        public void setPixel(int x, int y, int component,int value);
    }
 
 
    class ComponentSpec {
        int C, //Component id
        H, //Horizontal sampling factor
        V, //Vertical  ....
        Tq; //Quantization table destination selector
    }
 
 
    class FrameHeader {
        int SOF, //Start of frame in different type
        Lf, //Length
        P, //Sample Precision (from the orignal image)
        Y, //Number of lines
        X, //Number of samples per line
        Nf; //Number of component in the frame
 
        ComponentSpec Comp[]; //Components  C H V Tq
 
        public int get(InputStream in, int sof) throws Exception {
            //get data from file stream in
            //return 0 : correct       otherwise : error
 
            int i, temp, count = 0, c;
            SOF = sof;
            Lf = get16(in);
            count += 2;
            P = get8(in);
            count++;
            Y = get16(in);
            count += 2;
            height = Y;
            X = get16(in);
            count += 2;
            //width=X;
            Nf = get8(in);
            count++;
            Comp = new ComponentSpec[Nf + 1];
            for (i = 0; i <= Nf; i++) {
                Comp[i] = new ComponentSpec();
            }
            for (i = 1; i <= Nf; i++) {
                if (count > Lf) {
                    error("ERROR: frame format error");
                }
                c = get8(in);
                count++;
                if (c >= Lf) {
                    error("ERROR: fram format error [c>=Lf]");
                }
                Comp[c].C = c;
                temp = get8(in);
                count++;
                Comp[c].H = temp >> 4;
                Comp[c].V = temp & 0x0F;
                Comp[c].Tq = get8(in);
                count++;
            }
            if (count != Lf) {
                error("ERROR: frame format error [Lf!=count]");
            }
            return 1;
        }
    }
 
 
    class ScanComponent {
        int Cs, //Scan component selector
        Td, //DC table selector
        Ta; //AC table selector
    }
 
 
    class ScanHeader {
        int Ls, //length
        Ns, //Number of components in the scan
        Ss, //Start of spectral or predictor selection
        Se, //End of spectral selection
        Ah,
        Al;
 
        ScanComponent Comp[]; //Components Cs Td Ta
        // from [0] to [Ns-1]
        int get(InputStream in) throws Exception {
            //get data from file stream in
            //return 0 : correct       otherwise : error
 
            int i, temp, count = 0;
            Ls = get16(in);
            count += 2;
            Ns = get8(in);
            count++;
            Comp = new ScanComponent[Ns];
            for (i = 0; i < Ns; i++) {
                Comp[i] = new ScanComponent();
                if (count > Ls) {
                    error("ERROR: scan header format error");
                }
                Comp[i].Cs = get8(in);
                count++;
                temp = get8(in);
                count++;
                Comp[i].Td = temp >> 4;
                Comp[i].Ta = temp & 0x0F;
            }
            Ss = get8(in);
            count++;
            Se = get8(in);
            count++;
            temp = get8(in);
            count++;
            Ah = temp >> 4;
            Al = temp & 0x0F;
            if (count != Ls) {
                error("ERROR: scan header format error [count!=Ns]");
            }
            return 1;
        }
    }
 
 
    class QuantizationTable {
        int Lq, //length
        Pq[] = new int[4], //Quantization precision 8 or 16
        Tq[] = new int[4]; //1: this table is presented
        int Q[][] = new int[4][64]; //Tables
 
        public QuantizationTable() {
            Tq[0] = 0;
            Tq[1] = 0;
            Tq[2] = 0;
            Tq[3] = 0;
        }
 
        int get(InputStream in) throws Exception {
            //get data from file stream in
            //return 0 : correct       otherwise : error
 
            int i, count = 0, temp, t;
            Lq = get16(in);
            count += 2;
            while (count < Lq) {
                temp = get8(in);
                count++;
                t = temp & 0x0F;
                if (t > 3) {
                    error("ERROR: Quantization table ID > 3");
                }
                Pq[t] = temp >> 4;
                if (Pq[t] == 0) {
                    Pq[t] = 8;
                } else if (Pq[t] == 1) {
                    Pq[t] = 16;
                } else {
                    error("ERROR: Quantization table precision error");
                }
                Tq[t] = 1;
                if (Pq[t] == 8) {
                    for (i = 0; i < 64; i++) {
                        if (count > Lq) {
                            error("ERROR: Quantization table format error");
                        }
                        Q[t][i] = get8(in);
                        count++;
                    }
                    EnhanceQuantizationTable(Q[t]);
                } else {
                    for (i = 0; i < 64; i++) {
                        if (count > Lq) {
                            error("ERROR: Quantization table format error");
                        }
                        Q[t][i] = get16(in);
                        count += 2;
                    }
                    EnhanceQuantizationTable(Q[t]);
                }
            }
            if (count != Lq) {
                error("ERROR: Quantization table error [count!=Lq]");
            }
            return 1;
        }
    }
 
 
    class HuffmanTable {
        int Lh, //Length
        Tc[][] = new int[4][2], //1: this table is presented
        Th[] = new int[4], //1: this table is presented
        L[][][] = new int[4][2][16],
        V[][][][] = new int[4][2][16][200]; //tables
 
        public HuffmanTable() {
            Tc[0][0] = 0;
            Tc[1][0] = 0;
            Tc[2][0] = 0;
            Tc[3][0] = 0;
            Tc[0][1] = 0;
            Tc[1][1] = 0;
            Tc[2][1] = 0;
            Tc[3][1] = 0;
            Th[0] = 0;
            Th[1] = 0;
            Th[2] = 0;
            Th[3] = 0;
        }
 
        int get(InputStream in) throws Exception {
            //get data from file stream in
            //return 0 : correct       otherwise : error
 
            int i, j, temp, count = 0, t, c;
            Lh = get16(in);
            count += 2;
            while (count < Lh) {
                temp = get8(in);
                count++;
                t = temp & 0x0F;
                if (t > 3) {
                    error("ERROR: Huffman table ID > 3");
                }
                c = temp >> 4;
                if (c > 2) {
                    error("ERROR: Huffman table [Table class > 2 ]");
                }
                Th[t] = 1;
                Tc[t][c] = 1;
                for (i = 0; i < 16; i++) {
                    L[t][c][i] = get8(in);
                    count++;
                }
                for (i = 0; i < 16; i++) {
                    for (j = 0; j < L[t][c][i]; j++) {
                        if (count > Lh) {
                            error(
                                    "ERROR: Huffman table format error [count>Lh]");
                        }
                        V[t][c][i][j] = get8(in);
                        count++;
                    }
                }
            }
            if (count != Lh) {
                error("ERROR: Huffman table format error [count!=Lf]");
            }
            for (i = 0; i < 4; i++) {
                for (j = 0; j < 2; j++) {
                    if (Tc[i][j] != 0) {
                        Build_HuffTab(HuffTab[i][j], L[i][j], V[i][j]);
                    }
                }
            }
            return 1;
        }
    }
 
 
    private int readNumber(InputStream in) throws Exception {
        int Ld;
        Ld = get16(in);
        if (Ld != 4) {
            error("ERROR: Define number format error [Ld!=4]");
        }
        return get16(in);
    }
 
 
    private String readComment(InputStream in) throws Exception {
        int Lc, count = 0, i;
        StringBuffer sb = new StringBuffer();
 
        Lc = get16(in);
        count += 2;
        for (i = 0; count < Lc; i++) {
            sb.append((char) get8(in));
            count++;
        }
        return sb.toString();
    }
 
 
    private int readApp(InputStream in) throws Exception {
        int Lp;
        int count = 0;
        Lp = get16(in);
        count += 2;
        while (count < Lp) {
            get8(in);
            count++;
        }
        return Lp;
    }
 
 
    private final int get8(InputStream in) throws Exception {
        try {
            return in.read();
        } catch (IOException e) {
            error("get8() read error: " + e.toString());
            return -1;
        }
    }
 
    //get  16-bit data
    private final int get16(InputStream in) throws Exception {
        int temp;
        try {
            temp = in.read();
            temp <<= 8;
            return temp | in.read();
        } catch (IOException e) {
            error("get16() read error: " + e.toString());
            return -1;
        }
    }
 
 
    /********************************************************************
     Huffman table for fast search: (HuffTab) 8-bit Look up table
      2-layer search architecture, 1st-layer represent 256 node (8 bits)
      if codeword-length > 8 bits, then
          the entry of 1st-layer = (# of 2nd-layer table) | MSB
          and it is stored in the 2nd-layer
      Size of tables in each layer are 256.
      HuffTab[*][*][0-256] is always the only 1st-layer table.
 
      An entry can be:
      (1) (# of 2nd-layer table) | MSB , for code length > 8 in 1st-layer
      (2) (Code length) << 8 | HuffVal
     ********************************************************************/
    private int HuffTab[][][] = new int[4][2][MAX_HUFFMAN_SUBTREE * 256];
 
    /* Build_HuffTab()
        Parameter:  t       table ID
                    c       table class ( 0 for DC, 1 for AC )
                    L[i]    # of codewords which length is i
                    V[i][j] Huffman Value (length=i)
        Effect:
            build up HuffTab[t][c] using L and V.
     */
 
    private void Build_HuffTab(int tab[], int L[], int V[][]) throws Exception {
        int current_table, i, j, n, table_used, temp;
        int k;
        temp = 256;
        k = 0;
        for (i = 0; i < 8; i++) { // i+1 is Code length
            for (j = 0; j < L[i]; j++) {
                for (n = 0; n < (temp >> (i + 1)); n++) {
                    tab[k] = V[i][j] | ((i + 1) << 8);
                    k++;
                }
            }
        }
        for (i = 1; k < 256; i++, k++) {
            tab[k] = i | MSB;
        }
        if (i > 50) {
            error("ERROR: Huffman table out of memory!");
        }
        table_used = i;
        current_table = 1;
        k = 0;
        for (i = 8; i < 16; i++) { // i+1 is Code length
            for (j = 0; j < L[i]; j++) {
                for (n = 0; n < (temp >> (i - 7)); n++) {
                    tab[current_table * 256 + k] = V[i][j] | ((i + 1) << 8);
                    k++;
                }
                if (k >= 256) {
                    if (k > 256) {
                        error("ERROR: Huffman table error(1)!");
                    }
                    k = 0;
                    current_table++;
                }
            }
        }
    }
 
 
    /* HuffmanValue():
        return: Huffman Value of table
                0xFF?? if it receives a MARKER
        Parameter:  table   HuffTab[x][y] (ex) HuffmanValue(HuffTab[1][0],...)
                    temp    temp storage for remainded bits
                    index   index to bit of temp
                    in      FILE pointer
        Effect:
            temp  store new remainded bits
            index change to new index
            in    change to new position
        NOTE:
          Initial by   temp=0; index=0;
        NOTE: (explain temp and index)
          temp: is always in the form at calling time or returning time
           |  byte 4  |  byte 3  |  byte 2  |  byte 1  |
           |     0    |     0    | 00000000 | 00000??? |  if not a MARKER
                                                   ^index=3 (from 0 to 15)
                                                   321
        NOTE (marker and marker_index):
          If get a MARKER from 'in', marker=the low-byte of the MARKER
            and marker_index=9
          If marker_index=9 then index is always > 8, or HuffmanValue()
            will not be called.
     */
 
 
 
    private int HuffmanValue(int table[], int temp[], int index[],
                             InputStream in) throws Exception {
        int code, input, mask = 0xFFFF;
        if (index[0] < 8) {
            temp[0] <<= 8;
            input = get8(in);
            if (input == 0xFF) {
                marker = get8(in);
                if (marker != 0) {
                    marker_index = 9;
                }
            }
            temp[0] |= input;
        } else {
            index[0] -= 8;
        }
        code = table[temp[0] >> index[0]];
        if ((code & MSB) != 0) {
            if (marker_index != 0) {
                marker_index = 0;
                return 0xFF00 | marker;
            }
            temp[0] &= (mask >> (16 - index[0]));
            temp[0] <<= 8;
            input = get8(in);
            if (input == 0xFF) {
                marker = get8(in);
                if (marker != 0) {
                    marker_index = 9;
                }
            }
            temp[0] |= input;
            code = table[(code & 0xFF) * 256 + (temp[0] >> index[0])];
            index[0] += 8;
        }
        index[0] += 8 - (code >> 8);
        if (index[0] < 0) {
            error("index=" + index[0] + " temp=" + temp[0] + " code=" + code +
                  " in HuffmanValue()");
        }
        if (index[0] < marker_index) {
            marker_index = 0;
            return 0xFF00 | marker;
        }
        temp[0] &= (mask >> (16 - index[0]));
        return code & 0xFF;
    }
 

 
    private int getn(InputStream in, int n, int temp[], int index[]) throws
            Exception {
        int result, one = 1, n_one = -1;
        int mask = 0xFFFF, input;
        if (n == 0) {
            return 0;
        }
        index[0] -= n;
        if (index[0] >= 0) {
            if (index[0] < marker_index) {
                marker_index = 0;
                return (0xFF00 | marker) << 8;
            }
            result = temp[0] >> index[0];
            temp[0] &= (mask >> (16 - index[0]));
        } else {
            temp[0] <<= 8;
            input = get8(in);
            if (input == 0xFF) {
                marker = get8(in);
                if (marker != 0) {
                    marker_index = 9;
                }
            }
            temp[0] |= input;
            index[0] += 8;
            if (index[0] < 0) {
                if (marker_index != 0) {
                    marker_index = 0;
                    return (0xFF00 | marker) << 8;
                }
                temp[0] <<= 8;
                input = get8(in);
                if (input == 0xFF) {
                    marker = get8(in);
                    if (marker != 0) {
                        marker_index = 9;
                    }
                }
                temp[0] |= input;
                index[0] += 8;
            }
            if (index[0] < 0) {
                error("index=" + index[0] + " in getn()");
            }
            if (index[0] < marker_index) {
                marker_index = 0;
                return (0xFF00 | marker) << 8;
            }
            result = temp[0] >> index[0];
            temp[0] &= (mask >> (16 - index[0]));
        }
        if (result < (one << (n - 1))) {
            result += (n_one << n) + 1;
        }
        return result;
    }
 
   
 

 
 
    private void output(DCTArray out) {
        int temp_x, temp_8y, temp;
        int k = 0;
        int DU10[], DU20[];
        DU10 = DU[1][0];
        DU20 = DU[2][0];
 
        num++;
        for (int i = 0; i < YV; i++) {
            for (int j = 0; j < YH; j++) {
                temp_8y = i * 32;
                temp_x = temp = j * 4;
                for (int l = 0; l < 64; l++) {
                    if (x < Xsize && y < Ysize) {
                    	out.setPixel(x, y, 0, DU[0][k][l]);
                    	out.setPixel(x,y,1, DU10[temp_8y + temp_x]);
                    	out.setPixel(x,y,2,DU20[temp_8y + temp_x]);
                        
                    }
                    x++;
                    if ((x % YH) == 0) {
                        temp_x++;
                    }
                    if ((x % 8) == 0) {
                        y++;
                        x -= 8;
                        temp_x = temp;
                        if ((y % YV) == 0) {
                            temp_8y += 8;
                        }
                    }
                }
                k++;
                x += 8;
                y -= 8;
            }
            x -= YH * 8;
            y += 8;
        }
        x += YH * 8;
        y -= YV * 8;
        if (x >= Xsize) {
            y += YV * 8;
            x = 0;
        }
        yp = y;
    }
 
    private void level_shift(int du[], int P) throws Exception {
        int i;
        if (P == 8) {
            for (i = 0; i < 64; i++) {
                du[i] += 128;
            }
        } else if (P == 12) {
            for (i = 0; i < 64; i++) {
                du[i] += 2048;
            }
        } else {
            error("ERROR: Precision=" + P);
        }
    }
 
    
 
    private int decode_MCU(InputStream in, int PrevDC[],
                           int temp[], int index[]) throws Exception {
        int value, actab[], dctab[];
        int qtab[], Cs;
 
        for (Cs = 0; Cs < nComp; Cs++) {
            qtab = qTab[Cs];
            actab = acTab[Cs];
            dctab = dcTab[Cs];
            for (int i = 0; i < nBlock[Cs]; i++) {
                for (int k = 0; k < IDCT_Source.length; k++) {
                    IDCT_Source[k] = 0;
                }
                value = HuffmanValue(dctab, temp, index, in);
                if (value >= 0xFF00) {
                    return value;
                }
                PrevDC[Cs] = IDCT_Source[0] = PrevDC[Cs] +
                                              getn(in, value, temp, index);
                IDCT_Source[0] *= qtab[0];
                for (int j = 1; j < 64; j++) {
                    value = HuffmanValue(actab, temp, index, in);
                    if (value >= 0xFF00) {
                        return value;
                    }
                    j += (value >> 4);
                    if ((value & 0x0F) == 0) {
                        if ((value >> 4) == 0) {
                            break;
                        }
                    } else {
                        IDCT_Source[IDCT_P[j]] =
                                getn(in, value & 0x0F, temp, index) * qtab[j];
                    }
                }
                
                for( int i1 = 0; i1 <64; i1 ++ )
                {
                	DU[Cs][i][i1]=IDCT_Source[i1];

                	
                }
            }
        }
        return 0;
    }
 
 
// in-place operation
    private void EnhanceQuantizationTable(int qtab[]) {
 
        int i;
        for (i = 0; i < 8; i++) {
            qtab[table[0 * 8 + i]] *= 90;
            qtab[table[4 * 8 + i]] *= 90;
            qtab[table[2 * 8 + i]] *= 118;
            qtab[table[6 * 8 + i]] *= 49;
            qtab[table[5 * 8 + i]] *= 71;
            qtab[table[1 * 8 + i]] *= 126;
            qtab[table[7 * 8 + i]] *= 25;
            qtab[table[3 * 8 + i]] *= 106;
        }
        for (i = 0; i < 8; i++) {
            qtab[table[0 + 8 * i]] *= 90;
            qtab[table[4 + 8 * i]] *= 90;
            qtab[table[2 + 8 * i]] *= 118;
            qtab[table[6 + 8 * i]] *= 49;
            qtab[table[5 + 8 * i]] *= 71;
            qtab[table[1 + 8 * i]] *= 126;
            qtab[table[7 + 8 * i]] *= 25;
            qtab[table[3 + 8 * i]] *= 106;
        }
        for (i = 0; i < 64; i++) {
            qtab[i] >>= 6;
        }
    }
 

 
 
    public void decode(InputStream in, DCTArray out) throws Exception {
        int current, m, i, scan_num = 0, RST_num;
        int PRED[] = new int[10];
        if (in == null) {
            return;
        }
 
        x = 0;
        y = 0;
        yp = 0;
        num = 0;
        current = get16(in);
        if (current != 0xFFD8) { //SOI
            error("Not a JPEG file");
            return;
        }
        current = get16(in);
        while (current >> 4 != 0x0FFC) { //SOF 0~15
            switch (current) {
            case 0xFFC4: //DHT
                HT.get(in);
                break;
            case 0xFFCC: //DAC
                error(
                        "Program doesn't support arithmetic coding. (format error)");
                return;
            case 0xFFDB:
                QT.get(in);
                break;
            case 0xFFDD:
                Ri = readNumber(in);
                break;
            case 0xFFE0:
            case 0xFFE1:
            case 0xFFE2:
            case 0xFFE3:
            case 0xFFE4:
            case 0xFFE5:
            case 0xFFE6:
            case 0xFFE7:
            case 0xFFE8:
            case 0xFFE9:
            case 0xFFEA:
            case 0xFFEB:
            case 0xFFEC:
            case 0xFFED:
            case 0xFFEE:
            case 0xFFEF:
                readApp(in);
                break;
            case 0xFFFE:
                readComment(in);
                break;
            default:
                if (current >> 8 != 0xFF) {
                    error("ERROR: format error! (decode)");
                }
            }
            current = get16(in);
        }
        if (current < 0xFFC0 || current > 0xFFC7) {
            error("ERROR: could not handle arithmetic code!");
        }
 
        FH.get(in, current);
        current = get16(in);
 
        // pix = new int[FH.X * FH.Y];
        out.setSize(FH.X, FH.Y);
 
        do {
            while (current != 0x0FFDA) { //SOS
                switch (current) {
                case 0xFFC4: //DHT
                    HT.get(in);
                    break;
                case 0xFFCC: //DAC
                    error(
                            "Program doesn't support arithmetic coding. (format error)");
                case 0xFFDB:
                    QT.get(in);
                    break;
                case 0xFFDD:
                    Ri = readNumber(in);
                    break;
                case 0xFFE0:
                case 0xFFE1:
                case 0xFFE2:
                case 0xFFE3:
                case 0xFFE4:
                case 0xFFE5:
                case 0xFFE6:
                case 0xFFE7:
                case 0xFFE8:
                case 0xFFE9:
                case 0xFFEA:
                case 0xFFEB:
                case 0xFFEC:
                case 0xFFED:
                case 0xFFEE:
                case 0xFFEF:
                    readApp(in);
                    break;
                case 0xFFFE:
                    readComment(in);
                    break;
                default:
                    if (current >> 8 != 0xFF) {
                        error("ERROR: format error! (Parser.decode)");
                    }
                }
                current = get16(in);
            }
 
            SH.get(in);
            nComp = (int) SH.Ns;
            for (i = 0; i < nComp; i++) {
                int CompN = SH.Comp[i].Cs;
                qTab[i] = QT.Q[FH.Comp[CompN].Tq];
                nBlock[i] = FH.Comp[CompN].V * FH.Comp[CompN].H;
                dcTab[i] = HuffTab[SH.Comp[i].Td][0];
                acTab[i] = HuffTab[SH.Comp[i].Ta][1];
            }
            YH = FH.Comp[1].H;
            YV = FH.Comp[1].V;
            Xsize = FH.X;
            Ysize = FH.Y;
 
            scan_num++;
            m = 0;
 
            for (RST_num = 0; ; RST_num++) { //Decode one scan
                int MCU_num;
                int temp[] = new int[1]; // to store remainded bits
                int index[] = new int[1];
                temp[0] = 0;
                index[0] = 0;
                for (i = 0; i < 10; i++) {
                    PRED[i] = 0;
                    
                }
                if (Ri == 0) {
                    current = decode_MCU(in, PRED, temp, index);
                    // 0: correctly decoded
                    // otherwise: MARKER
                    while (current == 0) {
                        m++;
                        output(out);
                        current = decode_MCU(in, PRED, temp, index);
                    }
                    break; //current=MARKER
                }
                for (MCU_num = 0; MCU_num < Ri; MCU_num++) {
                    current = decode_MCU(in, PRED, temp, index);
                    output(out);
                    //fprintf(show,"%i ",MCU_num);
                    if (current != 0) {
                        break;
                    }
                }
                if (current == 0) {
                    if (marker_index != 0) {
                        current = (0xFF00 | marker);
                        marker_index = 0;
                    } else {
                        current = get16(in);
                    }
                }
                if (current >= 0xFFD0 && current <= 0xFFD7) {
                } else {
                    break; //current=MARKER
                }
            }
            if (current == 0xFFDC && scan_num == 1) { //DNL
                readNumber(in);
                current = get16(in);
            }
        } while (current != 0xFFD9);
    }
 
}