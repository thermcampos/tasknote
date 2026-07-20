package br.com.tasknoteapp.server.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.TextStyle;
import java.util.Locale;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimeAgoUtilTest {

  @Test
  void formatTest() {
    Assertions.assertEquals("1 hour ago", TimeAgoUtil.format(LocalDateTime.now().minusHours(1L)));
    Assertions.assertEquals("2 hours ago", TimeAgoUtil.format(LocalDateTime.now().minusHours(2L)));
    Assertions.assertEquals("1 day ago", TimeAgoUtil.format(LocalDateTime.now().minusDays(1L)));
    Assertions.assertEquals("2 days ago", TimeAgoUtil.format(LocalDateTime.now().minusDays(2L)));
    Assertions.assertEquals("1 month ago", TimeAgoUtil.format(LocalDateTime.now().minusMonths(1L)));
    Assertions.assertEquals(
        "2 months ago", TimeAgoUtil.format(LocalDateTime.now().minusMonths(2L)));
  }

  @Test
  void formatMidnightRolloverTest() {
    LocalDateTime now = LocalDateTime.of(2026, 5, 19, 0, 30); // 12:30 AM next day
    LocalDateTime pastTime = now.minusHours(1); // 11:30 PM previous day
    Assertions.assertEquals("1 hour ago", TimeAgoUtil.format(pastTime, now));

    // Test exactly 24 hours ago
    LocalDateTime twentyFourHoursAgo = now.minusDays(1);
    Assertions.assertEquals("1 day ago", TimeAgoUtil.format(twentyFourHoursAgo, now));

    // Test 23 hours ago across midnight
    LocalDateTime twentyThreeHoursAgo = now.minusHours(23); // 1:30 AM previous day
    Assertions.assertEquals("23 hours ago", TimeAgoUtil.format(twentyThreeHoursAgo, now));
  }

  @Test
  void formatDueDateTest() {
    Assertions.assertNull(TimeAgoUtil.formatDueDate(null));

    LocalDate localDate1 = LocalDate.now().plusDays(1L);
    String expected1 = "Due tomorrow" + getFormattedSuffix(localDate1);
    Assertions.assertEquals(expected1, TimeAgoUtil.formatDueDate(localDate1));

    LocalDate localDate2 = LocalDate.now().plusDays(12L);
    String expected2 = "12 days left" + getFormattedSuffix(localDate2);
    Assertions.assertEquals(expected2, TimeAgoUtil.formatDueDate(localDate2));
  }

  private String getFormattedSuffix(LocalDate futureDate) {
    String dayOfWeek = futureDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int dayOfMonth = futureDate.getDayOfMonth();
    String suffix = getDaySuffix(dayOfMonth);
    String month = futureDate.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    int year = futureDate.getYear();
    return String.format(" (%s %d%s, %s %d)", dayOfWeek, dayOfMonth, suffix, month, year);
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

  @Test
  void formatDueDateEdgeCasesTest() {
    // Test for today
    LocalDate today = LocalDate.now();
    String expectedToday = "Due today" + getFormattedSuffix(today);
    Assertions.assertEquals(expectedToday, TimeAgoUtil.formatDueDate(today));

    // Test for a past date
    LocalDate pastDate = LocalDate.now().minusDays(9L);
    Assertions.assertEquals(
        "Due" + getFormattedSuffix(pastDate), TimeAgoUtil.formatDueDate(pastDate));

    // Test for a far future date
    LocalDate farFutureDate = LocalDate.now().plusYears(5L);
    String expectedFarFuture = "5 years left" + getFormattedSuffix(farFutureDate);
    Assertions.assertEquals(expectedFarFuture, TimeAgoUtil.formatDueDate(farFutureDate));

    // Test for a leap year date
    LocalDate leapYearDate = LocalDate.of(2024, 2, 29);
    if (LocalDate.now().isBefore(leapYearDate)) {
      Period period = Period.between(LocalDate.now(), leapYearDate);
      String expectedLeapYear =
          String.format("%d days left", period.getDays()) + getFormattedSuffix(leapYearDate);
      Assertions.assertEquals(expectedLeapYear, TimeAgoUtil.formatDueDate(leapYearDate));
    }
  }
}
