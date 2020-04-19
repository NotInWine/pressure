package com.util;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;


public class MapUtil {

    /**
     * 将map 转为 实体
     *
     * @param map
     * @param t
     * @param <T>
     * @return
     */
    public static <T> T mapToBean(Map map, T t) {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(t.getClass());
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            for (PropertyDescriptor property : propertyDescriptors) {
                String key = property.getName();
                if (map.containsKey(key)) {
                    Object value = map.get(key);
                    // 得到property对应的setter方法
                    Method setter = property.getWriteMethod();
                    setter.invoke(t, value);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * list map 转为 list bean
     *
     * @param data
     * @param t
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public static <T> List<T> listToListBean(List<Map> data, Class<T> t) throws IllegalAccessException, InstantiationException {
        List<T> l = new ArrayList<T>();
        for (Map m : data) {
            T t1 = t.newInstance();
            l.add(MapUtil.mapToBean(m, t1));
        }
        return l;
    }

    /**
     * @param m
     * @return 将map中的value组成list返回
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List valueList(Map m) {
        List result = new ArrayList();
        try {
            Set s = m.entrySet();
            Iterator it = s.iterator();
            while (it != null && it.hasNext()) {
                Map.Entry entry = (Entry) it.next();
                result.add(m.get(entry.getKey()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List keyList(Map m) {
        List result = new ArrayList();
        try {
            Set s = m.entrySet();
            Iterator it = s.iterator();
            while (it != null && it.hasNext()) {
                Map.Entry entry = (Entry) it.next();
                result.add(entry.getKey());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map listToMap(List list, String keyValue) {
        Map data = new HashMap();
        Iterator it = list.iterator();
        while (it != null && it.hasNext()) {
            Map map = (Map) it.next();
            // 获取key
            Object key = map.get(keyValue);
            // 获取子集合
            List l = (List) data.get(key);

            if (l == null) {
                // 子集合的第一次初始化
                l = new ArrayList<>();
                data.put(key, l);
            }
            // 元素加入子集合
            l.add(map);
        }
        return data;
    }

    private static final String ITEMS_KEY = "items";

    /**
     * 给list 分组， 把被融合的记录坐位子元素 放置于 ITEM_KEY 对应的 list 中
     *
     * @param list
     * @param groupKey
     * @param s
     * @return [{...s,items:[{},{}]},{...s,items:[{},{}]},...]
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static List listGroup(List list, String groupKey, String... s) {
        Map data = new LinkedHashMap<>();
        Iterator it = list.iterator();
        while (it != null && it.hasNext()) {
            Map map = (Map) it.next();
            // 获取key
            Object key = map.get(groupKey);
            Map sMap = getlistGroupSon(data, map, key, s);
            // 获取子集合
            List l = (List) sMap.get(ITEMS_KEY);
            getSonMap(map, s);
            // 元素加入子集合
            l.add(map);
        }
        return new ArrayList(data.values());
    }



    private static void getSonMap(Map map, String... s) {
        for (String s1 :
                s) {
            map.remove(s1);
        }
    }

    private static Map getlistGroupSon(Map data, Map map, Object key, String... s) {
        // 获取key 对应的 map
        Map sMap = (Map) data.get(key);
        if (sMap == null) {
            // 子集合的第一次初始化
            sMap = new HashMap();
            for (String s1 :
                    s) {
                sMap.put(s1, map.remove(s1));
            }
            sMap.put(ITEMS_KEY, new ArrayList());
            data.put(key, sMap);
        }
        return sMap;
    }

    public static void main(String[] args) {
        List<Map<String, String>> list = new ArrayList<>();
        Map<String, String> map = new HashMap<>();
        map.put("type", "鞋子");
        map.put("fee", "22");
        Map<String, String> map1 = new HashMap<>();
        map1.put("type", "鞋子");
        map1.put("fee", "23");
        Map<String, String> map2 = new HashMap<>();
        map2.put("type", "袜子");
        map2.put("fee", "23");
        Map<String, String> map3 = new HashMap<>();
        map3.put("type", "裤子");
        map3.put("fee", "33");
        Map<String, String> map4 = new HashMap<>();
        map4.put("type", "裤子");
        map4.put("fee", "35");
        list.add(map);
        list.add(map1);
        list.add(map2);
        list.add(map4);
        list.add(map3);

    }
}
