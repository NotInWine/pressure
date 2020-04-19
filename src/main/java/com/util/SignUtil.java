package com.util;


import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.function.Function;

/**
 *
 * http 请求 签名工具
 * @author jryc
 *
 */
public class SignUtil {

	/**
	 *
	 * @param file
	 * @param newFile
	 * @param limit	批次处理字节长度
	 * @param function bytes 处理
	 */
    public static void doFinalFile(File file, File newFile, int limit, Function<byte[], byte[]> function) {
        try {
            InputStream is = new FileInputStream(file);
            OutputStream os = new FileOutputStream(newFile);
            byte[] bytes = new byte[limit];
            while (is.read(bytes) > 0) {
                byte[] e = function.apply(bytes);
                bytes = new byte[limit];
                os.write(e, 0, e.length);
            }
            os.close();
            is.close();
            System.out.println("write success");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 *
	 * @param file
	 * @param newFile
	 * @param limit	批次处理字节长度
	 * @param function bytes 处理
	 */
    public static void doFinalFileNIO(File file, File newFile, int limit, Function<byte[], byte[]> function) throws Exception {
		FileInputStream fin = null;
		FileOutputStream fos = null;
		try {
			fin = new FileInputStream(file);
			FileChannel channel = fin.getChannel();

			int capacity = limit;// 字节
			ByteBuffer bf = ByteBuffer.allocate(capacity);
			System.out.println("限制是：" + bf.limit() + "容量是：" + bf.capacity()
					+ "位置是：" + bf.position());

			fos = new FileOutputStream(newFile);
			FileChannel channelout = fos.getChannel();
			while (channel.read(bf) != -1) {
				/*
				 * 注意，读取后，将位置置为0，将limit置为容量, 以备下次读入到字节缓冲中，从0开始存储
				 */
				bf.clear();
				byte[] bytes = bf.array();
				// 写入数据
				channelout.write(ByteBuffer.wrap(function.apply(bytes)));
			}
			channel.close();
			System.out.println("write success");
        } finally {
			if (fin != null) {
				try {
					fin.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}



	/**
     * 把数组所有元素排序，并按照“参数=参数值”的模式用“&”字符拼接成字符串
     * @param params 需要排序并参与字符拼接的参数组
     * @return 拼接后字符串
     */
    public static String createLinkString(Map<String, String> params) {

        List<String> keys = new ArrayList<String>(params.keySet());
        Collections.sort(keys);

        String prestr = "";

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = params.get(key);

            if (i == keys.size() - 1) {//拼接时，不包括最后一个&字符
                prestr = prestr + key + "=" + value;
            } else {
                prestr = prestr + key + "=" + value + "&";
            }
        }

        return prestr;
    }

    public static void main(String[] args) throws Exception {
		//System.out.println(SignUtil.sign("73da7bb9d2a475bbc2ab79da7d4e94940cb9f9d5", SignType.SHA1));
		Map<String, String> params = new HashMap<>();
		params.put("noncestr", "Wm3WZYTPz0wzccnW");
		params.put("jsapi_ticket", "sM4AOVdWfPE4DxkXGEs8VMCPGGVi4C3VM0P37wVUCFvkVAy_90u5h9nbSlYy3-Sl-HhTdfl2fzFy1AOcHKP7qg");
		params.put("timestamp", "1414587457");
		params.put("url", "http://mp.weixin.qq.com?params=value");
	}
}
