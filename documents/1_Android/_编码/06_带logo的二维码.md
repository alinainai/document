中心带logo的二位码
```java
public class QrUtils {
    /**
     * 生成自定义二维码
     *
     * @param content     字符串内容
     * @param width       二维码宽度
     * @param height      二维码高度
     * @param color_black 黑色色块
     * @param color_white 白色色块
     * @param logoBitmap  logo图片（传null时不添加logo）
     * @param logoPercent logo所占百分比 [0.0,1.0]
     */
    public static Bitmap createQRCodeBitmap(String content, int width, int height,
                                            int color_black, int color_white, Bitmap logoBitmap, float logoPercent) {
        // 字符串内容判空
        if (TextUtils.isEmpty(content)) {
            return null;
        }
        // 宽和高>=0
        if (width < 0 || height < 0) {
            return null;
        }
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
            // 字符转码格式设置
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            // 容错率设置
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            // 空白边距设置
            hints.put(EncodeHintType.MARGIN, 0);
            /** 2.将配置参数传入到QRCodeWriter的encode方法生成BitMatrix(位矩阵)对象 */
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    //bitMatrix.get(x,y)方法返回true是黑色色块，false是白色色块
                    if (bitMatrix.get(x, y)) {// 黑色色块像素设置
                        pixels[y * width + x] = color_black;
                    } else {
                        pixels[y * width + x] = color_white;// 白色色块像素设置
                    }
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

            if (logoBitmap != null) {
                return addLogo(bitmap, logoBitmap, logoPercent);
            }
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 向二维码中间添加logo图片(图片合成)
     *
     * @param srcBitmap   原图片（生成的简单二维码图片）
     * @param logoBitmap  logo图片
     * @param logoPercent 百分比 (用于调整logo图片在原图片中的显示大小, 取值范围[0,1] )
     */
    private static Bitmap addLogo(Bitmap srcBitmap, Bitmap logoBitmap, float logoPercent) {
        if (srcBitmap == null) {
            return null;
        }
        if (logoBitmap == null) {
            return srcBitmap;
        }
        //传值不合法时使用0.2F
        if (logoPercent < 0F || logoPercent > 1F) {
            logoPercent = 0.2F;
        }

        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        int logoWidth = logoBitmap.getWidth();
        int logoHeight = logoBitmap.getHeight();

        float scaleWidth = srcWidth * logoPercent / logoWidth;
        float scaleHeight = srcHeight * logoPercent / logoHeight;

        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(srcBitmap, 0, 0, null);
        canvas.scale(scaleWidth, scaleHeight, srcWidth / 2F, srcHeight / 2F);
        canvas.drawBitmap(logoBitmap, srcWidth / 2F - logoWidth / 2F, srcHeight / 2F - logoHeight / 2F, null);

        return bitmap;
    }
}
// 使用
int qrSize = PixelUtils.getScreenWidth() * SCREEN_RATIO_CHILD / SCREEN_RATIO_MOTHER;
QrUtils.createQRCodeBitmap(content,qrSize,qrSize,
                BaseApplication.getInstance().getResources().getColor(R.color.color_253144_8AA5C6),
                BaseApplication.getInstance().getResources().getColor(R.color.color_FFFFFF_131F30),
                BitmapFactory.decodeResource(BaseApplication.getInstance().getResources(),R.drawable.ic_address_qr_logo),0.3F);
```
