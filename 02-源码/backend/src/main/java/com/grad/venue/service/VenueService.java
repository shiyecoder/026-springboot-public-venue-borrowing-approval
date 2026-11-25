package com.grad.venue.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.grad.venue.common.BizException;
import com.grad.venue.common.PageResult;
import com.grad.venue.entity.*;
import com.grad.venue.mapper.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 场地档案与物资：唯一性校验、被引用校验、详情聚合 */
@Service
public class VenueService {

    private final VenueMapper venueMapper;
    private final VenueItemMapper itemMapper;
    private final VenueApplicationMapper applyMapper;

    public VenueService(VenueMapper venueMapper, VenueItemMapper itemMapper, VenueApplicationMapper applyMapper) {
        this.venueMapper = venueMapper;
        this.itemMapper = itemMapper;
        this.applyMapper = applyMapper;
    }

    /** 分页查询：校区、容量、活动类型、状态、关键字 */
    public PageResult<Venue> page(long page, long size, String keyword, String campus, String status,
                                  String activityType, Integer minCapacity) {
        LambdaQueryWrapper<Venue> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(Venue::getName, keyword).or().like(Venue::getCode, keyword));
        }
        if (campus != null && !campus.isBlank()) w.eq(Venue::getCampus, campus);
        if (status != null && !status.isBlank()) w.eq(Venue::getStatus, status);
        if (activityType != null && !activityType.isBlank()) w.like(Venue::getAllowActivity, activityType);
        if (minCapacity != null) w.ge(Venue::getCapacity, minCapacity);
        w.orderByAsc(Venue::getCode);
        return PageResult.of(venueMapper.selectPage(new Page<>(page, size), w));
    }

    public List<Venue> options() {
        return venueMapper.selectList(new LambdaQueryWrapper<Venue>().eq(Venue::getStatus, "可用").orderByAsc(Venue::getCode));
    }

    public List<VenueItem> items(Long venueId) {
        return itemMapper.selectList(new LambdaQueryWrapper<VenueItem>().eq(VenueItem::getVenueId, venueId));
    }

    /** 新建/编辑场地：编号去空格后唯一，已产生申请单的场地不允许删除 */
    @Transactional
    public Venue save(Venue v) {
        if (v.getCode() == null || v.getCode().isBlank()) throw new BizException("场地编号必填");
        v.setCode(v.getCode().trim());
        if (v.getCode().length() > 50) throw new BizException("场地编号不得超过 50 个字符");
        LambdaQueryWrapper<Venue> w = new LambdaQueryWrapper<Venue>().eq(Venue::getCode, v.getCode());
        if (v.getId() != null) w.ne(Venue::getId, v.getId());
        if (venueMapper.selectCount(w) > 0) throw new BizException(409, "场地编号已存在");
        if (v.getCapacity() != null && v.getCapacity() < 0) throw new BizException("容量不得为负数");
        if (v.getId() == null) {
            v.setVersion(0);
            venueMapper.insert(v);
        } else {
            Venue old = venueMapper.selectById(v.getId());
            if (old == null) throw new BizException("场地不存在");
            if (old.getVersion() != null && v.getVersion() != null && !old.getVersion().equals(v.getVersion())) {
                throw new BizException(409, "数据已被他人更新，请刷新后重试");
            }
            venueMapper.updateById(v);
        }
        return v;
    }

    @Transactional
    public void delete(Long id) {
        Long cnt = applyMapper.selectCount(new LambdaQueryWrapper<VenueApplication>().eq(VenueApplication::getVenueId, id));
        if (cnt != null && cnt > 0) throw new BizException("该场地已被借用申请引用，不可删除");
        venueMapper.deleteById(id);
    }

    public Venue get(Long id) {
        Venue v = venueMapper.selectById(id);
        if (v == null) throw new BizException("场地不存在");
        return v;
    }

    @Transactional
    public VenueItem saveItem(VenueItem it) {
        if (it.getVenueId() == null) throw new BizException("请选择所属场地");
        if (it.getTotalQty() == null || it.getTotalQty() < 0) throw new BizException("物资数量不得为负");
        if (it.getId() == null) itemMapper.insert(it); else itemMapper.updateById(it);
        return it;
    }
}
