package br.com.tasknoteapp.server.controller;

import br.com.tasknoteapp.server.response.HomeItemsResponse;
import br.com.tasknoteapp.server.service.HomeService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** This class provides resources to handle home requests by the client. */
@RestController
@RequestMapping("/rest/home")
public class HomeController {

  private final HomeService homeService;

  public HomeController(HomeService homeService) {
    this.homeService = homeService;
  }

  /**
   * Get all existing tags, ordered alphabetically.
   *
   * @return List of String with the tags.
   */
  @GetMapping("/tasks/tags")
  public List<String> getTasksTags() {
    return homeService.getTopTasksTag();
  }

  /**
   * Get the Home items payload. Without search or tag filters it returns the default window;
   * with filters it runs an unbounded query composing all params with AND semantics.
   *
   * @param searchTerm Optional text to search by.
   * @param tag Optional tag name to filter by ("untagged" matches items without tags).
   * @param type Optional item type: all, tasks or notes.
   * @return {@link HomeItemsResponse} with the tasks and notes found.
   */
  @GetMapping("/items")
  public HomeItemsResponse getHomeItems(
      @RequestParam(name = "q", required = false) String searchTerm,
      @RequestParam(name = "tag", required = false) String tag,
      @RequestParam(name = "type", required = false) String type) {
    return homeService.getHomeItems(searchTerm, tag, type);
  }
}
