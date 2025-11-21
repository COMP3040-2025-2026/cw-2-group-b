package com.nottingham.mynottingham.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.nottingham.mynottingham.R
import com.nottingham.mynottingham.databinding.FragmentBookingDetailsBinding
import java.time.LocalDate

class BookingDetailsFragment : Fragment(R.layout.fragment_booking_details) {

    private var _binding: FragmentBookingDetailsBinding? = null
    private val binding get() = _binding!!
    
    // 获取 ViewModel
    private val viewModel: BookingViewModel by viewModels()

    private var currentFacilityName: String = ""
    private var selectedDate: LocalDate = LocalDate.now()
    private var selectedTimeSlot: Int? = null
    
    // 假设当前登录用户 (实际开发中应从 TokenManager 或 UserSession 获取)
    private val currentUserId = "user_001" 
    private val currentUserName = "Student A"

    companion object {
        private const val ARG_FACILITY_NAME = "facility_name"

        fun newInstance(facilityName: String): BookingDetailsFragment {
            val fragment = BookingDetailsFragment()
            val args = Bundle()
            args.putString(ARG_FACILITY_NAME, facilityName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // 获取传递过来的设施名称，如果没有则为空
            currentFacilityName = it.getString(ARG_FACILITY_NAME) ?: ""
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBookingDetailsBinding.bind(view)

        setupUI()
        setupDateList()
        setupTimeGrid()
        observeData()
        
        // 初始加载今天的预定数据
        viewModel.loadOccupiedSlots(currentFacilityName, selectedDate.toString())
    }

    private fun setupUI() {
        binding.tvFacilityName.text = currentFacilityName
        binding.toolbar.title = currentFacilityName
        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }
        
        binding.btnConfirmBooking.setOnClickListener {
            if (selectedTimeSlot != null) {
                confirmBooking()
            } else {
                Toast.makeText(context, "Please select a time slot", Toast.LENGTH_SHORT).show()
            }
        }
        // 初始状态禁用按钮
        binding.btnConfirmBooking.isEnabled = false
    }

    private fun setupDateList() {
        // 生成今天及未来6天，共7天
        val dateList = (0..6).map { LocalDate.now().plusDays(it.toLong()) }
        
        val dateAdapter = DateAdapter(dateList) { date ->
            selectedDate = date
            // 切换日期时，重置选中的时间段
            selectedTimeSlot = null
            binding.btnConfirmBooking.isEnabled = false
            
            // 重新从数据库加载这一天的预定情况
            viewModel.loadOccupiedSlots(currentFacilityName, date.toString())
        }

        binding.rvDates.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = dateAdapter
        }
    }

    private fun setupTimeGrid() {
        // 初始化时间网格 Adapter
        val timeAdapter = TimeSlotAdapter { slot ->
            selectedTimeSlot = slot
            // 只有选中有效时间段时才允许点击预定
            binding.btnConfirmBooking.isEnabled = (slot != null)
        }

        binding.rvTimeSlots.apply {
            layoutManager = GridLayoutManager(context, 3) // 3列显示
            adapter = timeAdapter
        }
    }

    private fun observeData() {
        // 监听数据库查询结果
        viewModel.occupiedSlots.observe(viewLifecycleOwner) { bookings ->
            // 拿到最新的预定列表，传给 Adapter 去处理显示(置灰)
            (binding.rvTimeSlots.adapter as? TimeSlotAdapter)?.updateBookings(bookings)
        }
    }

    private fun confirmBooking() {
        // 调用 ViewModel 保存数据到数据库
        viewModel.saveBooking(
            facilityName = currentFacilityName,
            date = selectedDate.toString(),
            timeSlot = selectedTimeSlot!!,
            userId = currentUserId,    // 这里应替换为真实的当前用户ID
            userName = currentUserName, // 这里应替换为真实的当前用户名
            onSuccess = {
                Toast.makeText(context, "Booking Successful!", Toast.LENGTH_SHORT).show()
                // 成功后可以选择返回上一页，或者只是刷新当前页面状态
                // parentFragmentManager.popBackStack() 
                
                // 这里为了体验，我们重置选择状态
                selectedTimeSlot = null
                binding.btnConfirmBooking.isEnabled = false
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
