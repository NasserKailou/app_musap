//package utils;
//
//
//import java.sql.Timestamp;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//
//import org.apache.commons.lang3.StringUtils;
//
///**
// * 
// * @author nasser
// *
// */
//public class Commons {
//
//	public Timestamp getDateT(String d) {
//
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//		LocalDate localDate = LocalDate.parse(d, formatter);
//		LocalDateTime dateV = null;
//
//		if (null != d && !d.trim().isEmpty()) {
//			try {
//				// System.out.println("la date est :"+dateV+tmpDate);
//
//				dateV = LocalDateTime.of(localDate, LocalTime.of(0, 0));
//
//			} catch (Exception e) {
//				e.getMessage();
//			}
//		}
//
//		return Timestamp.valueOf(dateV);
//
//	}
//
//    public static String getLastPart(String input, String wordSeparator) {
//        if (!StringUtils.contains(input, wordSeparator)) {
//            return input;
//        }
//        return StringUtils.substringAfterLast(input, wordSeparator);
//    }
//
//    //LocalDate
//
//    public static String localDateAsString(LocalDate date) {
//        return localDateAsString(date, "dd/MM/yyyy");
//    }
//
//    public static String localDateAsString(LocalDate localDate, String pattern) {
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
//        return formatter.format(localDate);
//    }
//
//    public static LocalDate stringAsLocalDate(String dateAsString) {
//        if (null == dateAsString) {
//            return null;
//        }
//        String lDateAsString = dateAsString.trim();
//        lDateAsString = lDateAsString
//                .replace("-", "/")
//                .replace(".", "/")
//                .replace(" ", "/");
//        if (lDateAsString.length() > 10) {
//            lDateAsString = lDateAsString.substring(0, 10);
//        }
//        return stringAsLocalDate(lDateAsString, "dd/MM/yyyy");
//    }
//
//
//    public static LocalDate stringAsLocalDate(String dateAsString, String pattern) {
//        if (null == dateAsString && null == pattern) {
//            return null;
//        }
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
//        return LocalDate.parse(dateAsString, formatter);
//    }
//    
//    public LocalDateTime stringToLocalDateTime(String dateAsString) {
//    	LocalDateTime dateToReturn = null;
//    	boolean dateError = false;
//    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    	LocalDate localDate = LocalDate.parse(dateAsString, formatter);
//    	
//    	if (null != dateAsString && !dateAsString.trim().isEmpty()) {
//			try {
//				System.out.println("la date est :"+dateAsString);
//
//				dateToReturn = LocalDateTime.of(localDate, LocalTime.of(0, 0));
//
//			} catch (Exception e) {
//				dateError = true;
//			}
//		}
//    	return dateToReturn;
//    }
//
//
//    //LocalDateTime
//
//    public static LocalDateTime shortStringAsLocalDateTime(String dateAsString) {
//        if (null == dateAsString) {
//            return null;
//        }
//        LocalDate localDate = stringAsLocalDate(dateAsString);
//        return LocalDateTime.of(localDate, LocalTime.of(0, 0));
//    }
//
//    public static String localDateTimeAsShortString(LocalDateTime localDateTime) {
//        if (null == localDateTime) {
//            return null;
//        }
//        return localDateAsString(localDateTime.toLocalDate(), "dd/MM/yyyy");
//    }
//
//    public static LocalDateTime stringAsLocalDateTime(String dateAsString, String pattern) {
//        if (null == dateAsString && null == pattern) {
//            return null;
//        }
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
//        return LocalDateTime.parse(dateAsString, formatter);
//    }
//
//    /**
//     * Check this before use
//     *
//     * @param localDateTime
//     * @return
//     */
//    public static String localDateTimeAsLongString(LocalDateTime localDateTime) {
//        if (null == localDateTime) {
//            return null;
//        }
//        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
//        return localDateTime.toString();
//    }
//}
