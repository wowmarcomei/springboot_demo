# Step 8: Thymeleaf模板与前端页面开发

## 学习目标

通过本步骤，你将学会：
- Thymeleaf模板引擎的使用和语法
- 创建响应式的Web用户界面
- 实现表单处理和数据绑定
- 静态资源管理和前后端交互
- Bootstrap框架集成和样式设计
- JavaScript与后端API的数据交互

## 前置要求

确保已完成：
- Step 1: 环境准备与OpenGauss安装
- Step 2: Spring Boot基础配置
- Step 3: MyBatis与OpenGauss集成
- Step 4: 实体类设计与数据库表创建
- Step 5: MyBatis Mapper接口与SQL映射开发
- Step 6: Service层业务逻辑实现
- Step 7: Controller层与REST API开发

## 1. Thymeleaf模板引擎介绍

### 1.1 Thymeleaf核心特性

**自然模板：** 模板文件可以在浏览器中直接打开查看
**强大的表达式：** 支持变量表达式、选择表达式、消息表达式等
**国际化支持：** 内置国际化功能
**缓存机制：** 生产环境自动缓存提升性能

### 1.2 基本语法

```html
<!-- 变量表达式 -->
<p th:text="${user.name}">用户名</p>

<!-- 链接表达式 -->
<a th:href="@{/user/{id}(id=${user.id})}">查看详情</a>

<!-- 条件判断 -->
<div th:if="${user.role == 'ADMIN'}">管理员功能</div>

<!-- 循环遍历 -->
<tr th:each="book : ${books}">
    <td th:text="${book.title}">书名</td>
</tr>

<!-- 表单绑定 -->
<form th:object="${user}" th:action="@{/users}" method="post">
    <input th:field="*{username}" type="text"/>
</form>
```

## 2. 页面控制器开发

### 2.1 创建页面控制器

创建`src/main/java/com/demo/library/controller/WebController.java`：

```java
package com.demo.library.controller;

import com.demo.library.dto.*;
import com.demo.library.service.UserService;
import com.demo.library.service.BookService;
import com.demo.library.service.BorrowService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * Web页面控制器
 */
@Controller
public class WebController {
    
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BorrowService borrowService;
    
    /**
     * 首页
     */
    @GetMapping("/")
    public String index(Model model) {
        logger.debug("访问首页");
        
        // 获取统计数据
        List<UserDTO> allUsers = userService.getAllUsers();
        List<BookDTO> allBooks = bookService.getAllBooks();
        List<BookDTO> availableBooks = bookService.getAvailableBooks();
        List<BorrowRecordDetailDTO> overdueRecords = borrowService.getOverdueRecords();
        
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalBooks", allBooks.size());
        model.addAttribute("availableBooks", availableBooks.size());
        model.addAttribute("overdueRecords", overdueRecords.size());
        
        // 获取最新图书
        List<BookDTO> newBooks = bookService.getNewBooks(30, 6);
        model.addAttribute("newBooks", newBooks);
        
        // 获取热门图书
        List<BookDTO> popularBooks = bookService.getPopularBooks(6);
        model.addAttribute("popularBooks", popularBooks);
        
        return "index";
    }
    
    /**
     * 登录页面
     */
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginDTO", new UserLoginDTO());
        return "auth/login";
    }
    
    /**
     * 处理登录
     */
    @PostMapping("/login")
    public String login(@Valid @ModelAttribute UserLoginDTO loginDTO,
                       BindingResult bindingResult,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        
        try {
            UserDTO user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
            // TODO: 保存用户登录状态到Session
            redirectAttributes.addFlashAttribute("message", "登录成功！");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/";
        } catch (Exception e) {
            logger.error("登录失败", e);
            model.addAttribute("error", e.getMessage());
            return "auth/login";
        }
    }
    
    /**
     * 注册页面
     */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("createDTO", new UserCreateDTO());
        return "auth/register";
    }
    
    /**
     * 处理注册
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute UserCreateDTO createDTO,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.createUser(createDTO);
            redirectAttributes.addFlashAttribute("message", "注册成功！请登录。");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("注册失败", e);
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
    
    /**
     * 图书列表页面
     */
    @GetMapping("/books")
    public String bookList(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "12") Integer pageSize,
                          @RequestParam(required = false) String keyword,
                          @RequestParam(required = false) String category,
                          @RequestParam(required = false) String author,
                          Model model) {
        
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setKeyword(keyword);
        queryDTO.setCategory(category);
        queryDTO.setAuthor(author);
        queryDTO.setAvailableOnly(true); // 只显示可借阅的图书
        
        PageInfo<BookDTO> pageInfo = bookService.getBooksByPage(pageNum, pageSize, queryDTO);
        List<String> categories = bookService.getAllCategories();
        
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedAuthor", author);
        
        return "books/list";
    }
    
    /**
     * 图书详情页面
     */
    @GetMapping("/books/{id}")
    public String bookDetail(@PathVariable Long id, Model model) {
        try {
            BookDTO book = bookService.getById(id);
            model.addAttribute("book", book);
            
            // 获取同分类的相关图书
            List<BookDTO> relatedBooks = bookService.getBooksByCategory(book.getCategory());
            relatedBooks.removeIf(b -> b.getId().equals(id)); // 排除当前图书
            if (relatedBooks.size() > 4) {
                relatedBooks = relatedBooks.subList(0, 4);
            }
            model.addAttribute("relatedBooks", relatedBooks);
            
            return "books/detail";
        } catch (Exception e) {
            logger.error("获取图书详情失败", e);
            model.addAttribute("error", "图书不存在或已被删除");
            return "error/404";
        }
    }
}
```

### 2.2 创建管理页面控制器

创建`src/main/java/com/demo/library/controller/AdminController.java`：

```java
package com.demo.library.controller;

import com.demo.library.dto.*;
import com.demo.library.service.UserService;
import com.demo.library.service.BookService;
import com.demo.library.service.BorrowService;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.List;

/**
 * 管理页面控制器
 */
@Controller
@RequestMapping("/admin")
public class AdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private BookService bookService;
    
    @Autowired
    private BorrowService borrowService;
    
    /**
     * 管理首页
     */
    @GetMapping
    public String adminIndex(Model model) {
        logger.debug("访问管理首页");
        
        // 获取统计数据
        List<UserDTO> allUsers = userService.getAllUsers();
        List<BookDTO> allBooks = bookService.getAllBooks();
        List<BookDTO> availableBooks = bookService.getAvailableBooks();
        List<BorrowRecordDetailDTO> overdueRecords = borrowService.getOverdueRecords();
        List<BorrowRecordDetailDTO> soonExpireRecords = borrowService.getSoonExpireRecords(3);
        
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalBooks", allBooks.size());
        model.addAttribute("availableBooks", availableBooks.size());
        model.addAttribute("overdueCount", overdueRecords.size());
        model.addAttribute("soonExpireCount", soonExpireRecords.size());
        
        // 获取最近借阅记录
        BorrowRecordQueryDTO queryDTO = new BorrowRecordQueryDTO();
        PageInfo<BorrowRecordDetailDTO> recentBorrows = borrowService.getBorrowRecordsByPage(1, 5, queryDTO);
        model.addAttribute("recentBorrows", recentBorrows.getList());
        
        return "admin/index";
    }
    
    /**
     * 用户管理页面
     */
    @GetMapping("/users")
    public String userManagement(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String role,
                                @RequestParam(required = false) String status,
                                Model model) {
        
        UserQueryDTO queryDTO = new UserQueryDTO();
        queryDTO.setKeyword(keyword);
        queryDTO.setRole(role);
        queryDTO.setStatus(status);
        
        PageInfo<UserDTO> pageInfo = userService.getUsersByPage(pageNum, pageSize, queryDTO);
        
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedStatus", status);
        
        return "admin/users/list";
    }
    
    /**
     * 用户详情页面
     */
    @GetMapping("/users/{id}")
    public String userDetail(@PathVariable Long id, Model model) {
        try {
            UserDTO user = userService.getById(id);
            List<BorrowRecordDetailDTO> borrowHistory = borrowService.getBorrowHistoryByUser(id);
            
            model.addAttribute("user", user);
            model.addAttribute("borrowHistory", borrowHistory);
            
            return "admin/users/detail";
        } catch (Exception e) {
            logger.error("获取用户详情失败", e);
            model.addAttribute("error", "用户不存在");
            return "error/404";
        }
    }
    
    /**
     * 添加用户页面
     */
    @GetMapping("/users/add")
    public String addUserPage(Model model) {
        model.addAttribute("createDTO", new UserCreateDTO());
        return "admin/users/add";
    }
    
    /**
     * 处理添加用户
     */
    @PostMapping("/users/add")
    public String addUser(@Valid @ModelAttribute UserCreateDTO createDTO,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        
        if (bindingResult.hasErrors()) {
            return "admin/users/add";
        }
        
        try {
            userService.createUser(createDTO);
            redirectAttributes.addFlashAttribute("message", "用户添加成功！");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin/users";
        } catch (Exception e) {
            logger.error("添加用户失败", e);
            model.addAttribute("error", e.getMessage());
            return "admin/users/add";
        }
    }
    
    /**
     * 编辑用户页面
     */
    @GetMapping("/users/{id}/edit")
    public String editUserPage(@PathVariable Long id, Model model) {
        try {
            UserDTO user = userService.getById(id);
            UserUpdateDTO updateDTO = new UserUpdateDTO();
            // 复制属性到updateDTO
            updateDTO.setEmail(user.getEmail());
            updateDTO.setFullName(user.getFullName());
            updateDTO.setPhone(user.getPhone());
            updateDTO.setAddress(user.getAddress());
            updateDTO.setRole(user.getRole());
            
            model.addAttribute("user", user);
            model.addAttribute("updateDTO", updateDTO);
            
            return "admin/users/edit";
        } catch (Exception e) {
            logger.error("获取用户信息失败", e);
            model.addAttribute("error", "用户不存在");
            return "error/404";
        }
    }
    
    /**
     * 处理编辑用户
     */
    @PostMapping("/users/{id}/edit")
    public String editUser(@PathVariable Long id,
                          @Valid @ModelAttribute UserUpdateDTO updateDTO,
                          BindingResult bindingResult,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        
        if (bindingResult.hasErrors()) {
            try {
                UserDTO user = userService.getById(id);
                model.addAttribute("user", user);
                return "admin/users/edit";
            } catch (Exception e) {
                model.addAttribute("error", "用户不存在");
                return "error/404";
            }
        }
        
        try {
            userService.updateUser(id, updateDTO);
            redirectAttributes.addFlashAttribute("message", "用户信息更新成功！");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin/users/" + id;
        } catch (Exception e) {
            logger.error("更新用户失败", e);
            model.addAttribute("error", e.getMessage());
            try {
                UserDTO user = userService.getById(id);
                model.addAttribute("user", user);
                return "admin/users/edit";
            } catch (Exception ex) {
                model.addAttribute("error", "用户不存在");
                return "error/404";
            }
        }
    }
    
    /**
     * 图书管理页面
     */
    @GetMapping("/books")
    public String bookManagement(@RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(required = false) String category,
                                @RequestParam(required = false) String status,
                                Model model) {
        
        BookQueryDTO queryDTO = new BookQueryDTO();
        queryDTO.setKeyword(keyword);
        queryDTO.setCategory(category);
        queryDTO.setStatus(status);
        
        PageInfo<BookDTO> pageInfo = bookService.getBooksByPage(pageNum, pageSize, queryDTO);
        List<String> categories = bookService.getAllCategories();
        
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("categories", categories);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("selectedStatus", status);
        
        return "admin/books/list";
    }
    
    /**
     * 添加图书页面
     */
    @GetMapping("/books/add")
    public String addBookPage(Model model) {
        model.addAttribute("createDTO", new BookCreateDTO());
        List<String> categories = bookService.getAllCategories();
        model.addAttribute("categories", categories);
        return "admin/books/add";
    }
    
    /**
     * 处理添加图书
     */
    @PostMapping("/books/add")
    public String addBook(@Valid @ModelAttribute BookCreateDTO createDTO,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        
        if (bindingResult.hasErrors()) {
            List<String> categories = bookService.getAllCategories();
            model.addAttribute("categories", categories);
            return "admin/books/add";
        }
        
        try {
            bookService.createBook(createDTO);
            redirectAttributes.addFlashAttribute("message", "图书添加成功！");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/admin/books";
        } catch (Exception e) {
            logger.error("添加图书失败", e);
            model.addAttribute("error", e.getMessage());
            List<String> categories = bookService.getAllCategories();
            model.addAttribute("categories", categories);
            return "admin/books/add";
        }
    }
    
    /**
     * 借阅管理页面
     */
    @GetMapping("/borrows")
    public String borrowManagement(@RequestParam(defaultValue = "1") Integer pageNum,
                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String userKeyword,
                                  @RequestParam(required = false) String bookKeyword,
                                  @RequestParam(required = false) Boolean overdue,
                                  Model model) {
        
        BorrowRecordQueryDTO queryDTO = new BorrowRecordQueryDTO();
        queryDTO.setStatus(status);
        queryDTO.setUserKeyword(userKeyword);
        queryDTO.setBookKeyword(bookKeyword);
        queryDTO.setOverdue(overdue);
        
        PageInfo<BorrowRecordDetailDTO> pageInfo = borrowService.getBorrowRecordsByPage(pageNum, pageSize, queryDTO);
        
        model.addAttribute("pageInfo", pageInfo);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("userKeyword", userKeyword);
        model.addAttribute("bookKeyword", bookKeyword);
        model.addAttribute("overdue", overdue);
        
        return "admin/borrows/list";
    }
    
    /**
     * 逾期管理页面
     */
    @GetMapping("/borrows/overdue")
    public String overdueManagement(Model model) {
        List<BorrowRecordDetailDTO> overdueRecords = borrowService.getOverdueRecords();
        List<BorrowRecordDetailDTO> soonExpireRecords = borrowService.getSoonExpireRecords(7);
        
        model.addAttribute("overdueRecords", overdueRecords);
        model.addAttribute("soonExpireRecords", soonExpireRecords);
        
        return "admin/borrows/overdue";
    }
}
```

## 3. 页面模板开发

### 3.1 创建基础布局模板

创建`src/main/resources/templates/layout/base.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title != null ? title + ' - 图书管理系统' : '图书管理系统'}">图书管理系统</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- 自定义样式 -->
    <link th:href="@{/css/main.css}" rel="stylesheet">
    
    <!-- 额外的页面样式 -->
    <th:block th:fragment="styles"></th:block>
</head>
<body>
    <!-- 导航栏 -->
    <nav th:replace="~{layout/navbar :: navbar}"></nav>
    
    <!-- 主要内容区域 -->
    <main class="flex-shrink-0">
        <!-- 页面标题 -->
        <div th:if="${pageTitle}" class="bg-light py-3">
            <div class="container">
                <h1 class="h3 mb-0" th:text="${pageTitle}">页面标题</h1>
                <nav th:if="${breadcrumbs}" aria-label="breadcrumb">
                    <ol class="breadcrumb mb-0 mt-2">
                        <li th:each="crumb, iterStat : ${breadcrumbs}" 
                            class="breadcrumb-item" 
                            th:classappend="${iterStat.last} ? 'active' : ''">
                            <a th:if="${not iterStat.last}" th:href="${crumb.url}" th:text="${crumb.name}">面包屑</a>
                            <span th:if="${iterStat.last}" th:text="${crumb.name}">当前页面</span>
                        </li>
                    </ol>
                </nav>
            </div>
        </div>
        
        <!-- 消息提示 -->
        <div th:if="${message}" class="container mt-3">
            <div class="alert alert-dismissible fade show" 
                 th:classappend="${messageType == 'success'} ? 'alert-success' : (${messageType == 'error'} ? 'alert-danger' : 'alert-info')">
                <span th:text="${message}">消息内容</span>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </div>
        
        <!-- 错误信息 -->
        <div th:if="${error}" class="container mt-3">
            <div class="alert alert-danger alert-dismissible fade show">
                <i class="bi bi-exclamation-triangle-fill me-2"></i>
                <span th:text="${error}">错误信息</span>
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        </div>
        
        <!-- 页面内容 -->
        <div th:fragment="content" class="container mt-3">
            <!-- 具体页面内容将在这里插入 -->
        </div>
    </main>
    
    <!-- 页脚 -->
    <footer th:replace="~{layout/footer :: footer}"></footer>
    
    <!-- Bootstrap JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <!-- jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- 自定义脚本 -->
    <script th:src="@{/js/main.js}"></script>
    
    <!-- 额外的页面脚本 -->
    <th:block th:fragment="scripts"></th:block>
</body>
</html>
```

### 3.2 创建导航栏组件

创建`src/main/resources/templates/layout/navbar.html`：

```html
<nav class="navbar navbar-expand-lg navbar-dark bg-primary" th:fragment="navbar">
    <div class="container">
        <a class="navbar-brand" th:href="@{/}">
            <i class="bi bi-book me-2"></i>
            图书管理系统
        </a>
        
        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navbarNav">
            <span class="navbar-toggler-icon"></span>
        </button>
        
        <div class="collapse navbar-collapse" id="navbarNav">
            <ul class="navbar-nav me-auto">
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/}" th:classappend="${#request.requestURI == '/'} ? 'active' : ''">
                        <i class="bi bi-house-door me-1"></i>首页
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/books}" th:classappend="${#strings.startsWith(#request.requestURI, '/books')} ? 'active' : ''">
                        <i class="bi bi-book me-1"></i>图书浏览
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#" th:classappend="${#strings.startsWith(#request.requestURI, '/my')} ? 'active' : ''">
                        <i class="bi bi-person me-1"></i>我的借阅
                    </a>
                </li>
                <!-- 管理员菜单 -->
                <li class="nav-item dropdown" sec:authorize="hasRole('ADMIN')">
                    <a class="nav-link dropdown-toggle" href="#" id="adminDropdown" role="button" data-bs-toggle="dropdown">
                        <i class="bi bi-gear me-1"></i>系统管理
                    </a>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" th:href="@{/admin}">
                            <i class="bi bi-speedometer2 me-2"></i>管理首页
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" th:href="@{/admin/users}">
                            <i class="bi bi-people me-2"></i>用户管理
                        </a></li>
                        <li><a class="dropdown-item" th:href="@{/admin/books}">
                            <i class="bi bi-book me-2"></i>图书管理
                        </a></li>
                        <li><a class="dropdown-item" th:href="@{/admin/borrows}">
                            <i class="bi bi-arrow-left-right me-2"></i>借阅管理
                        </a></li>
                    </ul>
                </li>
            </ul>
            
            <!-- 搜索框 -->
            <form class="d-flex me-3" th:action="@{/books}" method="get">
                <input class="form-control form-control-sm" type="search" name="keyword" placeholder="搜索图书..." th:value="${keyword}">
                <button class="btn btn-outline-light btn-sm ms-1" type="submit">
                    <i class="bi bi-search"></i>
                </button>
            </form>
            
            <!-- 用户菜单 -->
            <ul class="navbar-nav">
                <!-- 未登录状态 -->
                <li class="nav-item" sec:authorize="!isAuthenticated()">
                    <a class="nav-link" th:href="@{/login}">
                        <i class="bi bi-box-arrow-in-right me-1"></i>登录
                    </a>
                </li>
                <li class="nav-item" sec:authorize="!isAuthenticated()">
                    <a class="nav-link" th:href="@{/register}">
                        <i class="bi bi-person-plus me-1"></i>注册
                    </a>
                </li>
                
                <!-- 已登录状态 -->
                <li class="nav-item dropdown" sec:authorize="isAuthenticated()">
                    <a class="nav-link dropdown-toggle" href="#" id="userDropdown" role="button" data-bs-toggle="dropdown">
                        <i class="bi bi-person-circle me-1"></i>
                        <span sec:authentication="name">用户名</span>
                    </a>
                    <ul class="dropdown-menu">
                        <li><a class="dropdown-item" href="#">
                            <i class="bi bi-person me-2"></i>个人资料
                        </a></li>
                        <li><a class="dropdown-item" href="#">
                            <i class="bi bi-book-half me-2"></i>我的借阅
                        </a></li>
                        <li><a class="dropdown-item" href="#">
                            <i class="bi bi-gear me-2"></i>账户设置
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" th:href="@{/logout}">
                            <i class="bi bi-box-arrow-right me-2"></i>退出登录
                        </a></li>
                    </ul>
                </li>
            </ul>
        </div>
    </div>
</nav>
```

### 3.3 创建页脚组件

创建`src/main/resources/templates/layout/footer.html`：

```html
<footer class="bg-dark text-light mt-auto py-4" th:fragment="footer">
    <div class="container">
        <div class="row">
            <div class="col-lg-4 mb-3">
                <h5>图书管理系统</h5>
                <p class="text-muted">基于Spring Boot 3.x + MyBatis + OpenGauss构建的现代化图书管理系统。</p>
            </div>
            <div class="col-lg-2 mb-3">
                <h6>快速链接</h6>
                <ul class="list-unstyled">
                    <li><a href="#" class="text-muted">图书浏览</a></li>
                    <li><a href="#" class="text-muted">我的借阅</a></li>
                    <li><a href="#" class="text-muted">个人中心</a></li>
                </ul>
            </div>
            <div class="col-lg-2 mb-3">
                <h6>帮助中心</h6>
                <ul class="list-unstyled">
                    <li><a href="#" class="text-muted">使用指南</a></li>
                    <li><a href="#" class="text-muted">常见问题</a></li>
                    <li><a href="#" class="text-muted">联系我们</a></li>
                </ul>
            </div>
            <div class="col-lg-4 mb-3">
                <h6>联系信息</h6>
                <div class="text-muted">
                    <div><i class="bi bi-geo-alt me-2"></i>地址：北京市朝阳区某某街道123号</div>
                    <div><i class="bi bi-telephone me-2"></i>电话：010-12345678</div>
                    <div><i class="bi bi-envelope me-2"></i>邮箱：info@library.com</div>
                </div>
            </div>
        </div>
        <hr class="my-3">
        <div class="row align-items-center">
            <div class="col-md-8">
                <p class="text-muted mb-0">&copy; 2023 图书管理系统. All rights reserved.</p>
            </div>
            <div class="col-md-4 text-md-end">
                <a href="#" class="text-muted me-3"><i class="bi bi-github"></i></a>
                <a href="#" class="text-muted me-3"><i class="bi bi-twitter"></i></a>
                <a href="#" class="text-muted"><i class="bi bi-linkedin"></i></a>
            </div>
        </div>
    </div>
</footer>
```

## 4. 主要页面开发

### 4.1 创建首页

创建`src/main/resources/templates/index.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>首页 - 图书管理系统</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- 自定义样式 -->
    <link th:href="@{/css/main.css}" rel="stylesheet">
</head>
<body>
    <!-- 导航栏 -->
    <nav th:replace="~{layout/navbar :: navbar}"></nav>
    
    <!-- Hero Section -->
    <section class="bg-primary text-white py-5">
        <div class="container">
            <div class="row align-items-center">
                <div class="col-lg-6">
                    <h1 class="display-4 fw-bold mb-3">欢迎来到图书管理系统</h1>
                    <p class="lead mb-4">发现知识的海洋，探索无限可能。我们为您提供丰富的图书资源和便捷的借阅服务。</p>
                    <div class="d-grid d-sm-flex gap-3">
                        <a th:href="@{/books}" class="btn btn-light btn-lg px-4">
                            <i class="bi bi-search me-2"></i>浏览图书
                        </a>
                        <a th:href="@{/register}" class="btn btn-outline-light btn-lg px-4">
                            <i class="bi bi-person-plus me-2"></i>立即注册
                        </a>
                    </div>
                </div>
                <div class="col-lg-6 text-center">
                    <img src="/images/hero-books.svg" alt="图书" class="img-fluid" style="max-height: 300px;">
                </div>
            </div>
        </div>
    </section>
    
    <!-- 统计信息 -->
    <section class="py-5">
        <div class="container">
            <div class="row text-center">
                <div class="col-md-3 mb-4">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <i class="bi bi-people-fill text-primary display-4 mb-3"></i>
                            <h3 class="fw-bold text-primary" th:text="${totalUsers}">0</h3>
                            <p class="text-muted mb-0">注册用户</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <i class="bi bi-book-fill text-success display-4 mb-3"></i>
                            <h3 class="fw-bold text-success" th:text="${totalBooks}">0</h3>
                            <p class="text-muted mb-0">图书总数</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <i class="bi bi-bookmark-check-fill text-info display-4 mb-3"></i>
                            <h3 class="fw-bold text-info" th:text="${availableBooks}">0</h3>
                            <p class="text-muted mb-0">可借图书</p>
                        </div>
                    </div>
                </div>
                <div class="col-md-3 mb-4">
                    <div class="card border-0 shadow-sm h-100">
                        <div class="card-body">
                            <i class="bi bi-clock-fill text-warning display-4 mb-3"></i>
                            <h3 class="fw-bold text-warning" th:text="${overdueRecords}">0</h3>
                            <p class="text-muted mb-0">逾期记录</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    
    <!-- 新书推荐 -->
    <section class="py-5 bg-light">
        <div class="container">
            <div class="row mb-4">
                <div class="col-md-8">
                    <h2 class="fw-bold mb-3">最新图书</h2>
                    <p class="text-muted">发现最新上架的精彩图书</p>
                </div>
                <div class="col-md-4 text-md-end">
                    <a th:href="@{/books}" class="btn btn-outline-primary">
                        查看全部 <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>
            
            <div class="row" th:if="${newBooks != null and !newBooks.isEmpty()}">
                <div class="col-lg-4 col-md-6 mb-4" th:each="book : ${newBooks}">
                    <div class="card h-100 shadow-sm">
                        <img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/book-placeholder.jpg'}" 
                             class="card-img-top" 
                             style="height: 200px; object-fit: cover;"
                             th:alt="${book.title}">
                        <div class="card-body d-flex flex-column">
                            <h5 class="card-title" th:text="${book.title}">图书标题</h5>
                            <p class="text-muted small mb-2" th:text="${book.author}">作者</p>
                            <p class="card-text text-muted flex-grow-1" th:text="${#strings.abbreviate(book.description, 100)}">图书描述...</p>
                            <div class="d-flex justify-content-between align-items-center mt-auto">
                                <div>
                                    <span class="badge bg-primary" th:text="${book.category}">分类</span>
                                    <span class="badge bg-success ms-1" th:if="${book.availableCopies > 0}">可借阅</span>
                                </div>
                                <a th:href="@{/books/{id}(id=${book.id})}" class="btn btn-sm btn-outline-primary">
                                    查看详情
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div class="text-center py-5" th:if="${newBooks == null or newBooks.isEmpty()}">
                <i class="bi bi-book text-muted display-1"></i>
                <p class="text-muted mt-3">暂无新书上架</p>
            </div>
        </div>
    </section>
    
    <!-- 热门图书 -->
    <section class="py-5">
        <div class="container">
            <div class="row mb-4">
                <div class="col-md-8">
                    <h2 class="fw-bold mb-3">热门图书</h2>
                    <p class="text-muted">最受读者欢迎的图书推荐</p>
                </div>
                <div class="col-md-4 text-md-end">
                    <a th:href="@{/books}" class="btn btn-outline-primary">
                        查看全部 <i class="bi bi-arrow-right ms-1"></i>
                    </a>
                </div>
            </div>
            
            <div class="row" th:if="${popularBooks != null and !popularBooks.isEmpty()}">
                <div class="col-lg-4 col-md-6 mb-4" th:each="book : ${popularBooks}">
                    <div class="card h-100 border-0 shadow-sm">
                        <div class="row g-0">
                            <div class="col-4">
                                <img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/book-placeholder.jpg'}" 
                                     class="img-fluid rounded-start h-100" 
                                     style="object-fit: cover;"
                                     th:alt="${book.title}">
                            </div>
                            <div class="col-8">
                                <div class="card-body h-100 d-flex flex-column">
                                    <h6 class="card-title" th:text="${book.title}">图书标题</h6>
                                    <p class="text-muted small mb-2" th:text="${book.author}">作者</p>
                                    <div class="mt-auto">
                                        <span class="badge bg-warning text-dark">
                                            <i class="bi bi-star-fill me-1"></i>热门
                                        </span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>
    
    <!-- 消息提示 -->
    <div th:if="${message}" class="position-fixed bottom-0 end-0 p-3" style="z-index: 1050;">
        <div class="toast show" role="alert">
            <div class="toast-header">
                <i class="bi bi-info-circle-fill me-2 text-primary"></i>
                <strong class="me-auto">系统提示</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body" th:text="${message}">消息内容</div>
        </div>
    </div>
    
    <!-- 页脚 -->
    <footer th:replace="~{layout/footer :: footer}"></footer>
    
    <!-- JavaScript -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script th:src="@{/js/main.js}"></script>
</body>
</html>
```

### 4.2 创建登录页面

创建`src/main/resources/templates/auth/login.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>用户登录 - 图书管理系统</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- 自定义样式 -->
    <link th:href="@{/css/auth.css}" rel="stylesheet">
</head>
<body class="bg-light">
    <div class="container">
        <div class="row justify-content-center">
            <div class="col-md-6 col-lg-4">
                <div class="card shadow-lg border-0 mt-5">
                    <div class="card-body p-5">
                        <!-- Logo和标题 -->
                        <div class="text-center mb-4">
                            <i class="bi bi-book text-primary display-4"></i>
                            <h3 class="fw-bold text-dark mt-2">图书管理系统</h3>
                            <p class="text-muted">请登录您的账户</p>
                        </div>
                        
                        <!-- 错误信息 -->
                        <div th:if="${error}" class="alert alert-danger">
                            <i class="bi bi-exclamation-triangle-fill me-2"></i>
                            <span th:text="${error}">错误信息</span>
                        </div>
                        
                        <!-- 登录表单 -->
                        <form th:action="@{/login}" method="post" th:object="${loginDTO}">
                            <div class="mb-3">
                                <label for="username" class="form-label">用户名</label>
                                <div class="input-group">
                                    <span class="input-group-text">
                                        <i class="bi bi-person-fill"></i>
                                    </span>
                                    <input type="text" 
                                           class="form-control" 
                                           id="username" 
                                           th:field="*{username}"
                                           th:classappend="${#fields.hasErrors('username')} ? 'is-invalid' : ''"
                                           placeholder="请输入用户名">
                                </div>
                                <div class="invalid-feedback" th:if="${#fields.hasErrors('username')}" th:errors="*{username}">
                                    用户名错误信息
                                </div>
                            </div>
                            
                            <div class="mb-3">
                                <label for="password" class="form-label">密码</label>
                                <div class="input-group">
                                    <span class="input-group-text">
                                        <i class="bi bi-lock-fill"></i>
                                    </span>
                                    <input type="password" 
                                           class="form-control" 
                                           id="password" 
                                           th:field="*{password}"
                                           th:classappend="${#fields.hasErrors('password')} ? 'is-invalid' : ''"
                                           placeholder="请输入密码">
                                </div>
                                <div class="invalid-feedback" th:if="${#fields.hasErrors('password')}" th:errors="*{password}">
                                    密码错误信息
                                </div>
                            </div>
                            
                            <div class="mb-3 form-check">
                                <input type="checkbox" class="form-check-input" id="remember">
                                <label class="form-check-label" for="remember">记住我</label>
                            </div>
                            
                            <div class="d-grid">
                                <button type="submit" class="btn btn-primary btn-lg">
                                    <i class="bi bi-box-arrow-in-right me-2"></i>登录
                                </button>
                            </div>
                        </form>
                        
                        <!-- 其他链接 -->
                        <div class="text-center mt-4">
                            <p class="text-muted mb-2">还没有账户？</p>
                            <a th:href="@{/register}" class="btn btn-outline-primary">
                                <i class="bi bi-person-plus me-2"></i>立即注册
                            </a>
                        </div>
                        
                        <div class="text-center mt-3">
                            <a href="#" class="text-muted small">忘记密码？</a>
                        </div>
                    </div>
                </div>
                
                <!-- 返回首页链接 -->
                <div class="text-center mt-3">
                    <a th:href="@{/}" class="text-muted">
                        <i class="bi bi-arrow-left me-1"></i>返回首页
                    </a>
                </div>
            </div>
        </div>
    </div>
    
    <!-- JavaScript -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        // 表单验证和交互效果
        document.addEventListener('DOMContentLoaded', function() {
            const form = document.querySelector('form');
            const submitBtn = document.querySelector('button[type="submit"]');
            
            form.addEventListener('submit', function() {
                submitBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>登录中...';
                submitBtn.disabled = true;
            });
        });
    </script>
</body>
</html>
```

### 4.3 创建图书列表页面

创建`src/main/resources/templates/books/list.html`：

```html
<!DOCTYPE html>
<html lang="zh-CN" xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>图书浏览 - 图书管理系统</title>
    
    <!-- Bootstrap CSS -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <!-- Bootstrap Icons -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <!-- 自定义样式 -->
    <link th:href="@{/css/main.css}" rel="stylesheet">
</head>
<body>
    <!-- 导航栏 -->
    <nav th:replace="~{layout/navbar :: navbar}"></nav>
    
    <div class="container mt-4">
        <!-- 页面标题 -->
        <div class="row mb-4">
            <div class="col-md-8">
                <h2 class="fw-bold">图书浏览</h2>
                <p class="text-muted">共找到 <span class="fw-bold" th:text="${pageInfo.total}">0</span> 本图书</p>
            </div>
            <div class="col-md-4 text-md-end">
                <div class="btn-group">
                    <button type="button" class="btn btn-outline-secondary active" id="gridView">
                        <i class="bi bi-grid-3x3-gap"></i>
                    </button>
                    <button type="button" class="btn btn-outline-secondary" id="listView">
                        <i class="bi bi-list"></i>
                    </button>
                </div>
            </div>
        </div>
        
        <!-- 搜索和筛选 -->
        <div class="row mb-4">
            <div class="col-12">
                <div class="card">
                    <div class="card-body">
                        <form th:action="@{/books}" method="get" class="row g-3">
                            <div class="col-md-4">
                                <label for="keyword" class="form-label">关键词搜索</label>
                                <input type="text" 
                                       class="form-control" 
                                       id="keyword" 
                                       name="keyword" 
                                       th:value="${keyword}"
                                       placeholder="书名、作者、出版社...">
                            </div>
                            <div class="col-md-3">
                                <label for="category" class="form-label">图书分类</label>
                                <select class="form-select" id="category" name="category">
                                    <option value="">全部分类</option>
                                    <option th:each="cat : ${categories}" 
                                            th:value="${cat}" 
                                            th:text="${cat}"
                                            th:selected="${cat == selectedCategory}">分类</option>
                                </select>
                            </div>
                            <div class="col-md-3">
                                <label for="author" class="form-label">作者</label>
                                <input type="text" 
                                       class="form-control" 
                                       id="author" 
                                       name="author" 
                                       th:value="${selectedAuthor}"
                                       placeholder="作者姓名">
                            </div>
                            <div class="col-md-2 d-flex align-items-end">
                                <button type="submit" class="btn btn-primary w-100">
                                    <i class="bi bi-search me-1"></i>搜索
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 图书列表（网格视图） -->
        <div id="gridViewContent" class="row">
            <div class="col-lg-3 col-md-4 col-sm-6 mb-4" th:each="book : ${pageInfo.list}">
                <div class="card h-100 shadow-sm book-card">
                    <div class="position-relative">
                        <img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/book-placeholder.jpg'}" 
                             class="card-img-top" 
                             style="height: 250px; object-fit: cover;"
                             th:alt="${book.title}">
                        <div class="position-absolute top-0 end-0 m-2">
                            <span class="badge bg-success" th:if="${book.availableCopies > 0}">可借阅</span>
                            <span class="badge bg-secondary" th:if="${book.availableCopies == 0}">已借完</span>
                        </div>
                    </div>
                    
                    <div class="card-body d-flex flex-column">
                        <h6 class="card-title fw-bold" th:text="${book.title}">图书标题</h6>
                        <p class="text-muted small mb-1">
                            <i class="bi bi-person me-1"></i>
                            <span th:text="${book.author}">作者</span>
                        </p>
                        <p class="text-muted small mb-2">
                            <i class="bi bi-building me-1"></i>
                            <span th:text="${book.publisher}">出版社</span>
                        </p>
                        
                        <div class="mb-2">
                            <span class="badge bg-primary me-1" th:text="${book.category}">分类</span>
                            <span class="badge bg-info text-dark" th:text="${book.language}">语言</span>
                        </div>
                        
                        <p class="card-text text-muted small flex-grow-1" 
                           th:text="${#strings.abbreviate(book.description, 80)}">图书描述...</p>
                        
                        <div class="mt-auto">
                            <div class="d-flex justify-content-between align-items-center">
                                <small class="text-muted">
                                    <i class="bi bi-bookmark me-1"></i>
                                    可借：<span th:text="${book.availableCopies}">0</span>/<span th:text="${book.totalCopies}">0</span>
                                </small>
                                <div class="btn-group btn-group-sm">
                                    <a th:href="@{/books/{id}(id=${book.id})}" 
                                       class="btn btn-outline-primary btn-sm">
                                        <i class="bi bi-eye"></i>
                                    </a>
                                    <button class="btn btn-primary btn-sm" 
                                            th:if="${book.availableCopies > 0}"
                                            onclick="borrowBook([[${book.id}]])">
                                        <i class="bi bi-plus-circle"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 列表视图（默认隐藏） -->
        <div id="listViewContent" class="d-none">
            <div class="card" th:each="book : ${pageInfo.list}">
                <div class="card-body">
                    <div class="row align-items-center">
                        <div class="col-md-2">
                            <img th:src="${book.coverImageUrl != null ? book.coverImageUrl : '/images/book-placeholder.jpg'}" 
                                 class="img-fluid rounded" 
                                 style="max-height: 120px; object-fit: cover;"
                                 th:alt="${book.title}">
                        </div>
                        <div class="col-md-7">
                            <h5 class="fw-bold mb-2" th:text="${book.title}">图书标题</h5>
                            <p class="text-muted mb-1">
                                <i class="bi bi-person me-1"></i>作者：<span th:text="${book.author}">作者</span>
                                <span class="mx-2">|</span>
                                <i class="bi bi-building me-1"></i>出版社：<span th:text="${book.publisher}">出版社</span>
                            </p>
                            <p class="mb-2">
                                <span class="badge bg-primary me-1" th:text="${book.category}">分类</span>
                                <span class="badge bg-info text-dark me-1" th:text="${book.language}">语言</span>
                                <span class="badge bg-success" th:if="${book.availableCopies > 0}">可借阅</span>
                                <span class="badge bg-secondary" th:if="${book.availableCopies == 0}">已借完</span>
                            </p>
                            <p class="text-muted mb-0" th:text="${#strings.abbreviate(book.description, 150)}">图书描述...</p>
                        </div>
                        <div class="col-md-3 text-end">
                            <div class="mb-2">
                                <small class="text-muted">
                                    可借：<span class="fw-bold" th:text="${book.availableCopies}">0</span>/<span th:text="${book.totalCopies}">0</span>
                                </small>
                            </div>
                            <div class="btn-group">
                                <a th:href="@{/books/{id}(id=${book.id})}" class="btn btn-outline-primary">
                                    <i class="bi bi-eye me-1"></i>查看详情
                                </a>
                                <button class="btn btn-primary" 
                                        th:if="${book.availableCopies > 0}"
                                        onclick="borrowBook([[${book.id}]])">
                                    <i class="bi bi-plus-circle me-1"></i>借阅
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 空状态 -->
        <div class="text-center py-5" th:if="${pageInfo.list.isEmpty()}">
            <i class="bi bi-search text-muted" style="font-size: 4rem;"></i>
            <h4 class="text-muted mt-3">没有找到相关图书</h4>
            <p class="text-muted">请尝试调整搜索条件或浏览其他分类</p>
            <a th:href="@{/books}" class="btn btn-primary">查看所有图书</a>
        </div>
        
        <!-- 分页导航 -->
        <nav th:if="${pageInfo.pages > 1}" class="mt-4">
            <ul class="pagination justify-content-center">
                <li class="page-item" th:classappend="${pageInfo.isFirstPage} ? 'disabled' : ''">
                    <a class="page-link" th:href="@{/books(pageNum=${pageInfo.pageNum - 1}, keyword=${keyword}, category=${selectedCategory}, author=${selectedAuthor})}">
                        <i class="bi bi-chevron-left"></i>
                    </a>
                </li>
                
                <li class="page-item" th:each="nav : ${pageInfo.navigatepageNums}" 
                    th:classappend="${nav == pageInfo.pageNum} ? 'active' : ''">
                    <a class="page-link" 
                       th:href="@{/books(pageNum=${nav}, keyword=${keyword}, category=${selectedCategory}, author=${selectedAuthor})}"
                       th:text="${nav}">1</a>
                </li>
                
                <li class="page-item" th:classappend="${pageInfo.isLastPage} ? 'disabled' : ''">
                    <a class="page-link" th:href="@{/books(pageNum=${pageInfo.pageNum + 1}, keyword=${keyword}, category=${selectedCategory}, author=${selectedAuthor})}">
                        <i class="bi bi-chevron-right"></i>
                    </a>
                </li>
            </ul>
        </nav>
    </div>
    
    <!-- 借阅确认模态框 -->
    <div class="modal fade" id="borrowModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <i class="bi bi-plus-circle me-2"></i>借阅图书
                    </h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>确定要借阅这本图书吗？</p>
                    <div class="alert alert-info">
                        <i class="bi bi-info-circle me-2"></i>
                        借阅期限为30天，请按时归还。
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" id="confirmBorrow">确认借阅</button>
                </div>
            </div>
        </div>
    </div>
    
    <!-- 页脚 -->
    <footer th:replace="~{layout/footer :: footer}"></footer>
    
    <!-- JavaScript -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script>
        // 视图切换
        document.getElementById('gridView').addEventListener('click', function() {
            document.getElementById('gridViewContent').classList.remove('d-none');
            document.getElementById('listViewContent').classList.add('d-none');
            this.classList.add('active');
            document.getElementById('listView').classList.remove('active');
        });
        
        document.getElementById('listView').addEventListener('click', function() {
            document.getElementById('gridViewContent').classList.add('d-none');
            document.getElementById('listViewContent').classList.remove('d-none');
            this.classList.add('active');
            document.getElementById('gridView').classList.remove('active');
        });
        
        let selectedBookId = null;
        
        // 借阅图书
        function borrowBook(bookId) {
            selectedBookId = bookId;
            const modal = new bootstrap.Modal(document.getElementById('borrowModal'));
            modal.show();
        }
        
        // 确认借阅
        document.getElementById('confirmBorrow').addEventListener('click', function() {
            if (selectedBookId) {
                // TODO: 实现借阅逻辑，调用API
                console.log('借阅图书ID:', selectedBookId);
                
                // 模拟API调用
                fetch('/api/borrows', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        userId: 1, // TODO: 从用户会话获取
                        bookId: selectedBookId,
                        borrowNotes: ''
                    })
                })
                .then(response => response.json())
                .then(data => {
                    if (data.code === 200) {
                        alert('借阅成功！');
                        location.reload();
                    } else {
                        alert('借阅失败：' + data.message);
                    }
                })
                .catch(error => {
                    console.error('Error:', error);
                    alert('借阅失败，请稍后重试');
                });
                
                bootstrap.Modal.getInstance(document.getElementById('borrowModal')).hide();
            }
        });
    </script>
</body>
</html>
```

## 5. 静态资源管理

### 5.1 创建CSS样式文件

创建`src/main/resources/static/css/main.css`：

```css
/* 主要样式文件 */

/* 全局样式 */
body {
    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
    line-height: 1.6;
    color: #333;
}

.text-primary {
    color: #0066cc !important;
}

.bg-primary {
    background-color: #0066cc !important;
}

.btn-primary {
    background-color: #0066cc;
    border-color: #0066cc;
}

.btn-primary:hover {
    background-color: #0052a3;
    border-color: #0052a3;
}

/* 导航栏样式 */
.navbar-brand {
    font-weight: bold;
    font-size: 1.5rem;
}

.navbar-nav .nav-link {
    font-weight: 500;
    transition: color 0.3s ease;
}

.navbar-nav .nav-link:hover {
    color: rgba(255, 255, 255, 0.8) !important;
}

.navbar-nav .nav-link.active {
    color: #fff !important;
    font-weight: 600;
}

/* 卡片样式 */
.card {
    transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
}

.card:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1);
}

.book-card {
    border: none;
    border-radius: 8px;
    overflow: hidden;
}

.book-card .card-img-top {
    transition: transform 0.3s ease;
}

.book-card:hover .card-img-top {
    transform: scale(1.05);
}

/* 按钮样式 */
.btn {
    border-radius: 6px;
    font-weight: 500;
    transition: all 0.2s ease;
}

.btn:hover {
    transform: translateY(-1px);
}

.btn-group .btn {
    border-radius: 6px;
}

.btn-group .btn:not(:last-child) {
    margin-right: 2px;
}

/* 表单样式 */
.form-control, .form-select {
    border-radius: 6px;
    border: 1px solid #ddd;
    transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.form-control:focus, .form-select:focus {
    border-color: #0066cc;
    box-shadow: 0 0 0 0.2rem rgba(0, 102, 204, 0.25);
}

/* 徽章样式 */
.badge {
    font-weight: 500;
    border-radius: 4px;
}

/* 分页样式 */
.pagination {
    gap: 4px;
}

.page-link {
    border-radius: 6px;
    border: 1px solid #ddd;
    color: #0066cc;
    font-weight: 500;
}

.page-link:hover {
    background-color: #f8f9fa;
    border-color: #0066cc;
}

.page-item.active .page-link {
    background-color: #0066cc;
    border-color: #0066cc;
}

/* Hero section样式 */
.hero-section {
    background: linear-gradient(135deg, #0066cc 0%, #004499 100%);
    color: white;
}

/* 统计卡片样式 */
.stat-card {
    border: none;
    border-radius: 10px;
    padding: 1.5rem;
    text-align: center;
    transition: all 0.3s ease;
}

.stat-card:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
}

.stat-icon {
    font-size: 3rem;
    margin-bottom: 1rem;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .navbar-brand {
        font-size: 1.25rem;
    }
    
    .display-4 {
        font-size: 2rem;
    }
    
    .hero-section {
        padding: 3rem 0;
    }
    
    .stat-card {
        margin-bottom: 1rem;
    }
}

/* 动画效果 */
@keyframes fadeInUp {
    from {
        opacity: 0;
        transform: translateY(20px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.fade-in-up {
    animation: fadeInUp 0.6s ease-out;
}

/* 加载动画 */
.spinner-border-sm {
    width: 1rem;
    height: 1rem;
}

/* Toast消息样式 */
.toast {
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 自定义滚动条 */
::-webkit-scrollbar {
    width: 8px;
}

::-webkit-scrollbar-track {
    background: #f1f1f1;
}

::-webkit-scrollbar-thumb {
    background: #c1c1c1;
    border-radius: 4px;
}

::-webkit-scrollbar-thumb:hover {
    background: #a8a8a8;
}

/* 页脚样式 */
footer {
    margin-top: auto;
}

footer a {
    text-decoration: none;
    transition: color 0.2s ease;
}

footer a:hover {
    color: #fff !important;
}

/* 工具类 */
.text-truncate-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.text-truncate-3 {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.shadow-hover:hover {
    box-shadow: 0 8px 25px rgba(0, 0, 0, 0.15) !important;
}
```

创建`src/main/resources/static/css/auth.css`：

```css
/* 认证页面样式 */

body.bg-light {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    min-height: 100vh;
    display: flex;
    align-items: center;
}

.auth-card {
    border: none;
    border-radius: 15px;
    box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
    backdrop-filter: blur(10px);
    background: rgba(255, 255, 255, 0.95);
}

.auth-logo {
    width: 80px;
    height: 80px;
    background: linear-gradient(135deg, #0066cc 0%, #004499 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin: 0 auto 1rem;
    color: white;
    font-size: 2rem;
}

.input-group-text {
    background: #f8f9fa;
    border: 1px solid #ddd;
    color: #666;
}

.form-control {
    border-left: none;
}

.form-control:focus {
    box-shadow: none;
    border-color: #0066cc;
}

.form-control:focus + .input-group-text {
    border-color: #0066cc;
}

.btn-auth {
    padding: 12px 30px;
    font-weight: 600;
    border-radius: 8px;
    transition: all 0.3s ease;
}

.btn-auth:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(0, 102, 204, 0.3);
}

.auth-links a {
    color: #666;
    text-decoration: none;
    transition: color 0.2s ease;
}

.auth-links a:hover {
    color: #0066cc;
}

@media (max-width: 576px) {
    .auth-card {
        margin: 1rem;
        border-radius: 10px;
    }
}
```

### 5.2 创建JavaScript文件

创建`src/main/resources/static/js/main.js`：

```javascript
// 主要JavaScript文件

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 初始化工具提示
    initializeTooltips();
    
    // 初始化确认对话框
    initializeConfirmDialogs();
    
    // 初始化表单验证
    initializeFormValidation();
    
    // 初始化搜索功能
    initializeSearch();
    
    // 初始化通知
    initializeNotifications();
});

// 初始化工具提示
function initializeTooltips() {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    const tooltipList = [...tooltipTriggerList].map(tooltipTriggerEl => new bootstrap.Tooltip(tooltipTriggerEl));
}

// 初始化确认对话框
function initializeConfirmDialogs() {
    document.querySelectorAll('[data-confirm]').forEach(element => {
        element.addEventListener('click', function(e) {
            const message = this.getAttribute('data-confirm');
            if (!confirm(message)) {
                e.preventDefault();
                return false;
            }
        });
    });
}

// 初始化表单验证
function initializeFormValidation() {
    const forms = document.querySelectorAll('.needs-validation');
    
    Array.from(forms).forEach(form => {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            
            form.classList.add('was-validated');
        }, false);
    });
}

// 初始化搜索功能
function initializeSearch() {
    const searchInputs = document.querySelectorAll('.search-input');
    
    searchInputs.forEach(input => {
        let timeoutId;
        
        input.addEventListener('input', function() {
            clearTimeout(timeoutId);
            
            timeoutId = setTimeout(() => {
                const searchTerm = this.value.trim();
                if (searchTerm.length >= 2) {
                    performSearch(searchTerm);
                }
            }, 500);
        });
    });
}

// 执行搜索
function performSearch(searchTerm) {
    // 这里可以实现实时搜索功能
    console.log('搜索关键词:', searchTerm);
}

// 初始化通知
function initializeNotifications() {
    // 自动隐藏alert
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(alert => {
        if (alert.classList.contains('alert-dismissible')) {
            setTimeout(() => {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            }, 5000);
        }
    });
}

// API请求工具函数
const ApiClient = {
    baseURL: '/api',
    
    // GET请求
    get: async function(url, params = {}) {
        const urlWithParams = new URL(this.baseURL + url, window.location.origin);
        Object.keys(params).forEach(key => {
            if (params[key] !== null && params[key] !== undefined) {
                urlWithParams.searchParams.append(key, params[key]);
            }
        });
        
        try {
            const response = await fetch(urlWithParams);
            return await this.handleResponse(response);
        } catch (error) {
            console.error('GET请求错误:', error);
            throw error;
        }
    },
    
    // POST请求
    post: async function(url, data = {}) {
        try {
            const response = await fetch(this.baseURL + url, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(data)
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('POST请求错误:', error);
            throw error;
        }
    },
    
    // PUT请求
    put: async function(url, data = {}) {
        try {
            const response = await fetch(this.baseURL + url, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(data)
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('PUT请求错误:', error);
            throw error;
        }
    },
    
    // DELETE请求
    delete: async function(url) {
        try {
            const response = await fetch(this.baseURL + url, {
                method: 'DELETE',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            
            return await this.handleResponse(response);
        } catch (error) {
            console.error('DELETE请求错误:', error);
            throw error;
        }
    },
    
    // 处理响应
    handleResponse: async function(response) {
        const data = await response.json();
        
        if (data.code === 200) {
            return data;
        } else {
            throw new Error(data.message || '请求失败');
        }
    }
};

// 通知工具函数
const NotificationUtils = {
    // 显示成功消息
    success: function(message, duration = 3000) {
        this.show(message, 'success', duration);
    },
    
    // 显示错误消息
    error: function(message, duration = 5000) {
        this.show(message, 'error', duration);
    },
    
    // 显示警告消息
    warning: function(message, duration = 4000) {
        this.show(message, 'warning', duration);
    },
    
    // 显示信息消息
    info: function(message, duration = 3000) {
        this.show(message, 'info', duration);
    },
    
    // 显示通知
    show: function(message, type = 'info', duration = 3000) {
        const alertClass = {
            'success': 'alert-success',
            'error': 'alert-danger',
            'warning': 'alert-warning',
            'info': 'alert-info'
        };
        
        const iconClass = {
            'success': 'bi-check-circle-fill',
            'error': 'bi-exclamation-triangle-fill',
            'warning': 'bi-exclamation-triangle-fill',
            'info': 'bi-info-circle-fill'
        };
        
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert ${alertClass[type]} alert-dismissible fade show position-fixed`;
        alertDiv.style.cssText = 'top: 20px; right: 20px; z-index: 9999; min-width: 300px;';
        
        alertDiv.innerHTML = `
            <i class="bi ${iconClass[type]} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        `;
        
        document.body.appendChild(alertDiv);
        
        // 自动隐藏
        setTimeout(() => {
            if (alertDiv.parentNode) {
                const bsAlert = new bootstrap.Alert(alertDiv);
                bsAlert.close();
            }
        }, duration);
    }
};

// 工具函数
const Utils = {
    // 格式化日期
    formatDate: function(dateString, format = 'YYYY-MM-DD HH:mm:ss') {
        const date = new Date(dateString);
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        return format
            .replace('YYYY', year)
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hours)
            .replace('mm', minutes)
            .replace('ss', seconds);
    },
    
    // 防抖函数
    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    // 节流函数
    throttle: function(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },
    
    // 复制到剪贴板
    copyToClipboard: function(text) {
        if (navigator.clipboard) {
            navigator.clipboard.writeText(text).then(() => {
                NotificationUtils.success('已复制到剪贴板');
            }).catch(() => {
                NotificationUtils.error('复制失败');
            });
        } else {
            // 降级方案
            const textArea = document.createElement('textarea');
            textArea.value = text;
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();
            
            try {
                document.execCommand('copy');
                NotificationUtils.success('已复制到剪贴板');
            } catch (err) {
                NotificationUtils.error('复制失败');
            }
            
            document.body.removeChild(textArea);
        }
    }
};

// 加载状态管理
const LoadingManager = {
    show: function(element, text = '加载中...') {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        
        if (element) {
            const originalContent = element.innerHTML;
            element.setAttribute('data-original-content', originalContent);
            element.disabled = true;
            element.innerHTML = `<span class="spinner-border spinner-border-sm me-2"></span>${text}`;
        }
    },
    
    hide: function(element) {
        if (typeof element === 'string') {
            element = document.querySelector(element);
        }
        
        if (element) {
            const originalContent = element.getAttribute('data-original-content');
            if (originalContent) {
                element.innerHTML = originalContent;
                element.removeAttribute('data-original-content');
            }
            element.disabled = false;
        }
    }
};

// 暴露全局对象
window.ApiClient = ApiClient;
window.NotificationUtils = NotificationUtils;
window.Utils = Utils;
window.LoadingManager = LoadingManager;
```

## 6. 测试和调试

### 6.1 创建测试页面

创建测试控制器来验证页面功能：

```java
@Controller
@RequestMapping("/test")
public class TestController {
    
    @GetMapping("/components")
    public String testComponents(Model model) {
        // 测试各种组件的展示
        model.addAttribute("message", "这是一个测试消息");
        model.addAttribute("messageType", "success");
        return "test/components";
    }
}
```

### 6.2 浏览器测试

1. **启动应用**：`mvn spring-boot:run`
2. **访问页面**：
   - 首页：http://localhost:8080/
   - 登录：http://localhost:8080/login
   - 注册：http://localhost:8080/register
   - 图书列表：http://localhost:8080/books

### 6.3 响应式测试

使用浏览器开发者工具测试不同屏幕尺寸下的页面展示效果。

## 7. 性能优化

### 7.1 静态资源优化

```yaml
# application.yml
spring:
  # 静态资源配置
  web:
    resources:
      # 静态资源缓存时间
      cache:
        cachecontrol:
          max-age: 7d
      # 静态资源路径
      static-locations:
        - classpath:/static/
        - classpath:/public/
  
  # Thymeleaf配置
  thymeleaf:
    # 开发环境关闭缓存
    cache: false
    # 模板编码
    encoding: UTF-8
    # 模板模式
    mode: HTML
    # 模板前缀
    prefix: classpath:/templates/
    # 模板后缀
    suffix: .html
```

### 7.2 模板片段复用

合理使用Thymeleaf的fragment功能，避免重复代码。

### 7.3 懒加载实现

对图片等资源实现懒加载：

```html
<img class="lazy" data-src="/path/to/image.jpg" alt="图片">
```

```javascript
// 懒加载实现
const lazyImages = document.querySelectorAll('.lazy');
const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const img = entry.target;
            img.src = img.dataset.src;
            img.classList.remove('lazy');
            observer.unobserve(img);
        }
    });
});

lazyImages.forEach(img => imageObserver.observe(img));
```

## 8. 总结

通过本步骤，我们完成了：

✅ **Thymeleaf模板开发**
- 学习Thymeleaf语法和最佳实践
- 创建基础布局和组件模板
- 实现模板片段复用

✅ **响应式页面设计**
- 使用Bootstrap 5构建响应式界面
- 创建美观的用户体验
- 实现移动设备适配

✅ **完整的页面功能**
- 首页展示和统计信息
- 用户认证页面（登录、注册）
- 图书浏览和搜索功能
- 管理后台界面

✅ **前后端交互**
- 表单数据绑定和验证
- AJAX API调用
- 实时搜索和筛选

✅ **静态资源管理**
- CSS样式组织和优化
- JavaScript模块化开发
- 图片和资源优化

✅ **用户体验优化**
- 消息提示和错误处理
- 加载状态和动画效果
- 无障碍访问支持

## 总体完成情况

至此，我们已经完成了Spring Boot图书管理系统的完整开发流程：

1. ✅ **Step 1-4**: 环境准备、配置、数据库设计和实体类开发
2. ✅ **Step 5**: MyBatis数据访问层开发  
3. ✅ **Step 6**: Service业务逻辑层开发
4. ✅ **Step 7**: Controller REST API开发
5. ✅ **Step 8**: Thymeleaf前端页面开发

这个完整的学习项目涵盖了现代Spring Boot应用开发的核心技术栈，包括数据访问、业务逻辑、REST API和前端页面，是一个很好的学习和实践项目。

---

**提示**：前端开发要注重用户体验和页面性能，合理使用缓存、懒加载等技术。同时要确保页面的可访问性和响应式设计，为不同设备的用户提供良好的使用体验。