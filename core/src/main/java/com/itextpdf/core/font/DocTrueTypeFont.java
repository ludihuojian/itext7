package com.itextpdf.core.font;

import com.itextpdf.basics.IntHashtable;
import com.itextpdf.basics.font.FontEncoding;
import com.itextpdf.basics.font.TrueTypeFont;
import com.itextpdf.basics.font.cmap.CMapToUnicode;
import com.itextpdf.basics.font.otf.Glyph;
import com.itextpdf.core.pdf.PdfArray;
import com.itextpdf.core.pdf.PdfDictionary;
import com.itextpdf.core.pdf.PdfName;
import com.itextpdf.core.pdf.PdfNumber;
import com.itextpdf.core.pdf.PdfStream;
import com.itextpdf.core.pdf.PdfString;

import java.io.IOException;
import java.util.Map;

class DocTrueTypeFont extends TrueTypeFont implements DocFontProgram {

    private PdfStream fontFile;
    private PdfName fontFileName;
    private PdfName subtype;

    private DocTrueTypeFont(PdfDictionary fontDictionary) {
        super();
        PdfName baseFontName = fontDictionary.getAsName(PdfName.BaseFont);
        if (baseFontName != null) {
            getFontNames().setFontName(baseFontName.getValue());
        } else {
            getFontNames().setFontName(DocFontUtils.createRandomFontName());
        }
        subtype = fontDictionary.getAsName(PdfName.Subtype);
    }

    static TrueTypeFont createFontProgram(PdfDictionary fontDictionary, FontEncoding fontEncoding) {
        DocTrueTypeFont fontProgram = new DocTrueTypeFont(fontDictionary);
        fillFontDescriptor(fontProgram, fontDictionary.getAsDictionary(PdfName.FontDescriptor));

        PdfNumber firstCharNumber = fontDictionary.getAsNumber(PdfName.FirstChar);
        int firstChar = firstCharNumber != null ? Math.max(firstCharNumber.getIntValue(), 0) : 0;
        int[] widths = DocFontUtils.convertSimpleWidthsArray(fontDictionary.getAsArray(PdfName.Widths), firstChar);
        for (int i = 0; i < 256; i++) {
            Glyph glyph = new Glyph(i, widths[i], fontEncoding.getUnicode(i));
            fontProgram.codeToGlyph.put(i, glyph);
            if (glyph.getUnicode() != null) {
                fontProgram.unicodeToGlyph.put(glyph.getUnicode(), glyph);
            }
        }
        return fontProgram;
    }

    static TrueTypeFont createFontProgram(PdfDictionary fontDictionary, CMapToUnicode toUnicode) {
        DocTrueTypeFont fontProgram = new DocTrueTypeFont(fontDictionary);
        PdfDictionary fontDescriptor = fontDictionary.getAsDictionary(PdfName.FontDescriptor);
        fillFontDescriptor(fontProgram, fontDescriptor);

        Map<Integer, Integer> cid2Uni = null;
        try {
            cid2Uni = toUnicode.createDirectMapping();
        } catch (IOException ignored) { }

        int dw = fontDescriptor != null && fontDescriptor.containsKey(PdfName.DW)
                ? fontDescriptor.getAsInt(PdfName.DW) : 1000;
        if (cid2Uni != null) {
            IntHashtable widths = DocFontUtils.convertCompositeWidthsArray(fontDictionary.getAsArray(PdfName.Widths));
            for (Map.Entry<Integer, Integer> entry : cid2Uni.entrySet()) {
                int width = widths.containsKey(entry.getKey()) ? widths.get(entry.getKey()) : dw;
                Glyph glyph = new Glyph(entry.getKey(), width, entry.getValue());
                fontProgram.codeToGlyph.put(entry.getKey(), glyph);
                fontProgram.unicodeToGlyph.put(glyph.getUnicode(), glyph);
            }
        }

        if (fontProgram.codeToGlyph.get(0) == null) {
            fontProgram.codeToGlyph.put(0, new Glyph(0, dw, (Integer) null));
        }
        return fontProgram;
    }


    public PdfStream getFontFile() {
        return fontFile;
    }

    public PdfName getFontFileName() {
        return fontFileName;
    }

    public PdfName getSubtype() {
        return subtype;
    }

    static void fillFontDescriptor(DocTrueTypeFont font, PdfDictionary fontDesc) {
        if (fontDesc == null) {
            return;
        }
        PdfNumber v = fontDesc.getAsNumber(PdfName.Ascent);
        if (v != null) {
            font.setTypoAscender(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.Descent);
        if (v != null) {
            font.setTypoDescender(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.CapHeight);
        if (v != null) {
            font.setCapHeight(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.XHeight);
        if (v != null) {
            font.setXHeight(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.ItalicAngle);
        if (v != null) {
            font.setItalicAngle(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.StemV);
        if (v != null) {
            font.setStemV(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.StemH);
        if (v != null) {
            font.setStemH(v.getIntValue());
        }
        v = fontDesc.getAsNumber(PdfName.FontWeight);
        if (v != null) {
            font.setFontWeight(v.getIntValue());
        }

        PdfName fontStretch = fontDesc.getAsName(PdfName.FontStretch);
        if (fontStretch != null) {
            font.setFontWidth(fontStretch.getValue());
        }


        PdfArray bboxValue = fontDesc.getAsArray(PdfName.FontBBox);

        if (bboxValue != null) {
            int[] bbox = new int[4];
            //llx
            bbox[0] = bboxValue.getAsNumber(0).getIntValue();
            //lly
            bbox[1] = bboxValue.getAsNumber(1).getIntValue();
            //urx
            bbox[2] = bboxValue.getAsNumber(2).getIntValue();
            //ury
            bbox[3] = bboxValue.getAsNumber(3).getIntValue();

            if (bbox[0] > bbox[2]) {
                int t = bbox[0];
                bbox[0] = bbox[2];
                bbox[2] = t;
            }
            if (bbox[1] > bbox[3]) {
                int t = bbox[1];
                bbox[1] = bbox[3];
                bbox[3] = t;
            }
            font.setBbox(bbox);
        }

        PdfString fontFamily = fontDesc.getAsString(PdfName.FontFamily);
        if (fontFamily != null) {
            font.setFontFamily(fontFamily.getValue());
        }

        PdfNumber flagsValue = fontDesc.getAsNumber(PdfName.Flags);
        if (flagsValue != null) {
            int flags = flagsValue.getIntValue();
            if ((flags & 1) != 0) {
                font.setFixedPitch(true);
            }
            if ((flags & 262144) != 0) {
                font.setBold(true);
            }
        }

        PdfName[] fontFileNames = new PdfName[] {PdfName.FontFile, PdfName.FontFile2, PdfName.FontFile3};
        for (PdfName fontFile: fontFileNames) {
            if (fontDesc.containsKey(fontFile)) {
                font.fontFileName = fontFile;
                font.fontFile = fontDesc.getAsStream(fontFile);
                break;
            }
        }
    }

}
