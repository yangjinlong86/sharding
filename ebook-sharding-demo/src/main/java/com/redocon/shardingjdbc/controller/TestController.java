package com.redocon.shardingjdbc.controller;

import com.redocon.shardingjdbc.entity.*;
import com.redocon.shardingjdbc.mapper.*;
import com.redocon.shardingjdbc.service.LibraryEbookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
public class TestController {

    @Resource
    private LibraryEbookService libraryEbookService;

    @Autowired
    private CustomerMapper customerMapper;

    @Autowired
    private LibraryMapper libraryMapper;

    @Autowired
    private EbookMapper ebookMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderEbookMapper orderEbookMapper;

    @Autowired
    private OrderLibraryMapper orderLibraryMapper;

    @Autowired
    private LibraryEbookMapper libraryEbookMapper;
    
    @Resource
    private LibraryEbookAllMapper libraryEbookAllMapper;

    public String autoRun(){
        // 清除记录
        clear();

        // 新增客户
        for (int i = 1; i <=4 ; i++) {
            addCustomer("TEST-CUSTOMER-"+i);
        }

        List<Customer> customers = customerMapper.select();
        // 新增馆
        addLibraries(customers);
        // 新增电子书
        addEbooks();
        // 新增订单
        addOrders(customers);

        List<Order> orders = orderMapper.select();

        addOrderEbookAndLibraries(orders);
        // 配发电子书到馆
        addLibraryEbook(orders);
        return "auto run complete!";
    }

    @DeleteMapping("/truncate")
    public String clear(){
        libraryEbookMapper.truncate();
        orderLibraryMapper.truncate();
        orderEbookMapper.truncate();
        orderMapper.truncate();
        ebookMapper.truncate();
        libraryMapper.truncate();
        customerMapper.truncate();
        return "truncate complete!";
    }


    /**
     * 新建客户
     * @param name
     */
    public void addCustomer(@PathVariable("name") String name) {
        customerMapper.insert(name);
    }

    /**
     * 新建图书馆
     */
    public String addLibraries(List<Customer> customers) {
        for (Customer customer : customers) {
            for (int i = 1; i <= 250; i++) {
                libraryMapper.insert(customer.getId(), customer.getName() + "图书馆" + i);
            }
        }
        return "add libraries complete!";
    }

    /**
     * 新增电子书库存
     * @return
     */
    public String addEbooks() {
        for (int i = 1; i <= 20000; i++) {
            ebookMapper.insert("电子书" + i);
        }
        return "新增电子书成功!";
    }

    /**
     * 新增订单
     * @return
     */
    public String addOrders(List<Customer> customers) {
        customers.forEach(customer -> {
            orderMapper.insert(customer.getId(), customer.getName()+"的订单");
        });

        return "新增订单成功!";
    }

    /**
     * 选择订单对应的图书馆和电子书
     * @return
     */
    public String addOrderEbookAndLibraries(List<Order> orders) {
        orders.forEach(order -> {
            // 查询所有的电子书
            List<Ebook> ebookList = ebookMapper.select();
            System.out.println("电子书共： "+ ebookList.size());
            // 插入order_ebook表
            ebookList.forEach(ebook -> orderEbookMapper.insert(ebook.getId(), order.getCustomerId(), order.getId()));
            System.out.println("add order ebooks success!");

            // 查询客户下所有的图书馆
            List<Library> libraryList = libraryMapper.selectByCustomerId(order.getCustomerId());
            // 插入order_library
            libraryList.forEach(library -> orderLibraryMapper.insert(order.getCustomerId(), library.getId(), order.getId()));
            System.out.println("add order libraries success!");
        });


        return "add order ebooks and libraries complete!";
    }

    /**
     * 配发电子书到图书馆
     * @return
     */
    public String addLibraryEbook(List<Order> orders){
        orders.forEach(order -> {
            System.out.println("清除配发记录");
            // 避免重复配发，首先删除订单对应的配发记录
            libraryEbookMapper.deleteByOrderId(order.getId());

            System.out.println("查询订单相关的图书馆");
            // 根据orderId找到libraryId
            List<OrderLibrary> orderLibraryList = orderLibraryMapper.selectByOrderId(order.getId());

            System.out.println("查询订单相关的电子书");
            // 根据orderId找ebookId
            List<OrderEbook> orderEbookList = orderEbookMapper.selectByOrderId(order.getId());

            System.out.println("配发电子书到图书馆");

            List<LibraryEbook> libraryEbookList = new ArrayList<>();
            // 将所有的电子书配发到客户下所有的图书馆
            orderLibraryList.forEach(orderLibrary -> {
                orderEbookList.forEach(orderEbook -> {
                    LibraryEbook libraryEbook = new LibraryEbook();
                    libraryEbook.setOrderId(order.getId());
                    libraryEbook.setCustomerId(order.getCustomerId());
                    libraryEbook.setLibraryId(orderLibrary.getLibraryId());
                    libraryEbook.setEbookId(orderEbook.getEbookId());
                    libraryEbook.setStatus(1);
                    libraryEbookList.add(libraryEbook);
                });
            });
            libraryEbookMapper.insertBatch(libraryEbookList);
        });
        return "配发电子书完成！";
    }


    @GetMapping("/order/{orderId}")
    public Order orderList(@PathVariable long orderId){
        System.out.println("hello");
        return orderMapper.selectById(orderId);
    }

    @GetMapping("/queryByOrderId/{orderId}/{pageNum}/{sizePerPage}")
    List<Map> queryByOrderId(@PathVariable("orderId") Integer orderId, @PathVariable("pageNum") Integer pageNum, @PathVariable("sizePerPage") Integer sizePerPage){
        return libraryEbookService.queryByOrderId(orderId, (pageNum-1) * sizePerPage, sizePerPage);
    }

    @GetMapping("/queryByLibraryId/{libraryId}/{pageNum}/{sizePerPage}")
    List<Map> queryByLibraryId(@PathVariable("libraryId") Integer libraryId, @PathVariable("pageNum") Integer pageNum, @PathVariable("sizePerPage") Integer sizePerPage){
        return libraryEbookService.queryByLibraryId(libraryId, (pageNum-1) * sizePerPage, sizePerPage);
    }

    @GetMapping("/queryByCustomerId/{customerId}/{pageNum}/{sizePerPage}")
    List<Map> queryByCustomerId(@PathVariable("customerId") Integer customerId, @PathVariable("pageNum") Integer pageNum, @PathVariable("sizePerPage") Integer sizePerPage){
        return libraryEbookService.queryByCustomerId(customerId, (pageNum-1) * sizePerPage, sizePerPage);
    }
    
    @PostMapping("/batchInsertIntoLibraryEbookAll")
    void insertSlect(){

        for (int i = 1; i <= 6666; i++) {
            List<LibraryEbook> list = libraryEbookAllMapper.selectSharding(i*1000, 1000);
            libraryEbookAllMapper.insertBatch(list);
        }

    }
    
}
