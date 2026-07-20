package br.com.tasknoteapp.server.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Objects;

/** This class contains util methods to format local date time. */
public class TimeAgoUtil {

  /**
   * Format a time in the time ago format.
   *
   * @param pastTime The pastime to be formatted.
   * @return String with formatted time.
   */
  public static String format(LocalDateTime pastTime) {
    return format(pastTime, LocalDateTime.now());
  }

  /**
   * Format a time in the time ago format.
   *
   * @param pastTime The pastime to be formatted.
   * @param now The current time.
   * @return String with formatted time.
   */
  public static String format(LocalDateTime pastTime, LocalDateTime now) {
    if (Objects.isNull(pastTime)) {
      return "Some time ago";
    }
    Period period = Period.between(pastTime.toLocalDate(), now.toLocalDate());
    Duration duration = Duration.between(pastTime, now);
    if (period.getYears() > 1) {
      return String.format("%d years ago", period.getYears());
    } else if (period.getYears() > 0) {
      return String.format("%d year ago", period.getYears());
    } else if (period.getMonths() > 1) {
      return String.format("%d months ago", period.getMonths());
    } else if (period.getMonths() > 0) {
      return String.format("%d month ago", period.getMonths());
    } else if (duration.toHours() >= 24) {
      if (period.getDays() > 1) {
        return String.format("%d days ago", period.getDays());
      } else {
        return String.format("%d day ago", period.getDays());
      }
    } else if (duration.toHours() > 1L) {
      return String.format("%d hours ago", duration.toHours());
    } else if (duration.toHours() > 0L) {
      return String.format("%d hour ago", duration.toHours());
    } else if (duration.toMinutes() > 1L) {
      return String.format("%d minutes ago", duration.toMinutes());
    } else if (duration.toMinutes() > 0L) {
      return String.format("%d minute ago", duration.toMinutes());
    } else if (duration.toSeconds() > 1L) {
      return String.format("%d seconds ago", duration.toSeconds());
    } else {
      return "Moments ago";
    }
  }

  /**
   * Format a given due date into left time format.
   *
   * @param futureDate Date to be formatted in the yyyy-MM-dd format.
   * @return The formatted date.
   */
  public static String formatDueDate(LocalDate futureDate) {
    if (Objects.isNull(futureDate)) {
      return null;
    }

    StringBuilder sb = new StringBuilder();

    // Format should be: yyyy-MM-dd
    Period period = Period.between(LocalDate.now(), futureDate);
    if (period.getYears() > 1) {
      sb.append(String.format("%d years left", period.getYears()));
    } else if (period.getYears() > 0) {
      sb.append(String.format("%d year left", period.getYears()));
    } else if (period.getMonths() > 1) {
      sb.append(String.format("%d months left", period.getMonths()));
    } else if (period.getMonths() > 0) {
      sb.append(String.format("%d month left", period.getMonths()));
    } else if (period.getDays() > 1) {
      sb.append(String.format("%d days left", period.getDays()));
    } else if (period.getDays() > 0) {
      sb.append("Due tomorrow");
    } else if (period.getDays() == 0) {
      sb.append("Due today");
    } else {
      sb.append("Due");
    }

    String dayOfWeek = futureDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int dayOfMonth = futureDate.getDayOfMonth();
    String suffix = getDaySuffix(dayOfMonth);
    String month = futureDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int year = futureDate.getYear();
    String dateFmt = String.format(" (%s %d%s, %s %d)", dayOfWeek, dayOfMonth, suffix, month, year);

    return sb + dateFmt;
  }

  private static String getDaySuffix(int day) {
    if (day >= 11 && day <= 13) {
      return "th";
    }
    return switch (day % 10) {
      case 1 -> "st";
      case 2 -> "nd";
      case 3 -> "rd";
      default -> "th";
    };
  }
}
