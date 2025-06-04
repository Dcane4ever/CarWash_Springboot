package com.Carwash.test;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.beans.factory.annotation.Autowired;
import com.Carwash.test.model.User;
import com.Carwash.test.repository.UserRepository;
import com.Carwash.test.util.SessionManager;
import com.Carwash.test.util.PasswordEncoder;
import java.time.LocalDateTime;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.Carwash.test.model.Booking;
import com.Carwash.test.repository.BookingRepository;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;
import org.springframework.web.bind.annotation.PathVariable;
import java.time.LocalTime;
import com.Carwash.test.model.ServicePrice;
import com.Carwash.test.repository.ServicePriceRepository;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import com.Carwash.test.service.EmailService;
import java.util.UUID;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class WelcomeController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private SessionManager sessionManager;
	
	@Autowired
	private BookingRepository bookingRepository;
	
	@Autowired
	private ServicePriceRepository servicePriceRepository;
	
	@Autowired
	private EmailService emailService;
	
	@GetMapping("/")
	public String root() {
		return "redirect:/Welcome";
	}
	
	@GetMapping("/Welcome")
	public String welcome(Model model) {
		if (sessionManager.isLoggedIn()) {
			return "redirect:/dashboard";
		}
		return "Welcome";
	}
	
	@GetMapping("/user/login")
	public String login() {
		if (sessionManager.isLoggedIn()) {
			return "redirect:/dashboard";
		}
		return "Login";
	}

	@GetMapping("/user/register")
	public String registerPage() {
		if (sessionManager.isLoggedIn()) {
			return "redirect:/Welcome";
		}
		return "Register";
	}
	
	@PostMapping("/user/register")
	public String registerUser(
			@RequestParam("username") String username,
			@RequestParam("email") String email,
			@RequestParam("password") String password,
			Model model,
			HttpServletRequest request) {
		
		try {
			// Check if username or email already exists
			if (userRepository.findByUsername(username) != null) {
				model.addAttribute("error", "Username already exists!");
				return "Register";
			}
			
			if (userRepository.findByEmail(email) != null) {
				model.addAttribute("error", "Email already exists!");
				return "Register";
			}
			
			// Create new user
			User user = new User();
			user.setUsername(username);
			user.setEmail(email);
			user.setPassword(PasswordEncoder.encode(password));
			user.setEnabled(false);
			
			// Generate verification token
			String token = UUID.randomUUID().toString();
			user.setVerificationToken(token);
			user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
			
			// Save user
			userRepository.save(user);
			
			// Send verification email
			String baseUrl = getBaseUrl(request);
			emailService.sendVerificationEmail(email, token, baseUrl);
			
			model.addAttribute("message", "Registration successful! Please check your email to verify your account.");
			
		} catch (Exception e) {
			model.addAttribute("error", "An error occurred during registration!");
			return "Register";
		}
		
		return "Register";
	}

	@GetMapping("/user/verify")
	public String verifyUserForm(@RequestParam("token") String token, Model model) {
		// Instead of processing, show a form that will submit the token
		model.addAttribute("token", token);
		return "verify-email";  // We'll create this template
	}

	@PostMapping("/user/verify")
	public String verifyUser(@RequestParam("token") String token, Model model) {
		User user = userRepository.findByVerificationToken(token);
		
		if (user == null) {
			model.addAttribute("error", "Invalid verification token!");
			return "Login";
		}
		
		if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
			model.addAttribute("error", "Verification token has expired!");
			return "Login";
		}
		
		user.setEnabled(true);
		user.setVerificationToken(null);
		user.setTokenExpiryDate(null);
		userRepository.save(user);
		
		model.addAttribute("message", "Email verified successfully! You can now login.");
		return "Login";
	}

	@PostMapping("/user/login")
	public String loginUser(
			@RequestParam("username") String usernameOrEmail,
			@RequestParam("password") String password,
			Model model) {
		
		try {
			User user = userRepository.findByUsername(usernameOrEmail);
			if (user == null) {
				user = userRepository.findByEmail(usernameOrEmail);
			}
			
			if (user == null) {
				model.addAttribute("error", "Account not found. Please check your username/email or register.");
				return "Login";
			}
			
			if (!PasswordEncoder.matches(password, user.getPassword())) {
				model.addAttribute("error", "Incorrect password. Please try again.");
				return "Login";
			}
			
			if (!user.isEnabled()) {
				model.addAttribute("error", "Please verify your email before logging in!");
				return "Login";
			}
			
			sessionManager.setUserSession(user.getUsername());
			return "redirect:/dashboard";
			
		} catch (Exception e) {
			model.addAttribute("error", "An error occurred during login. Please try again later.");
			return "Login";
		}
	}

	@GetMapping("/user/logout")
	public String logout() {
		sessionManager.logout();
		return "redirect:/user/login";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		String username = sessionManager.getLoggedInUser();
		if (username == null) {
			return "redirect:/user/login";
		}
		
		User user = userRepository.findByUsername(username);
		model.addAttribute("username", user.getUsername());
		model.addAttribute("userEmail", user.getEmail());
		
		// Add service prices
		Map<String, Double> prices = servicePriceRepository.findAll().stream()
			.collect(Collectors.toMap(
				ServicePrice::getServiceType,
				ServicePrice::getPrice
			));
		model.addAttribute("prices", prices);

		// Add slot information
		Map<Integer, Map<String, String>> slots = new HashMap<>();
		slots.put(1, Map.of("start", "08:00", "end", "09:00"));
		slots.put(2, Map.of("start", "11:00", "end", "12:00"));
		slots.put(3, Map.of("start", "14:00", "end", "15:00"));
		slots.put(4, Map.of("start", "16:00", "end", "17:00"));
		model.addAttribute("slots", slots);
		
		return "dashboard";
	}

	@PostMapping("/book-appointment")
	public String bookAppointment(@ModelAttribute Booking booking, 
							@RequestParam("appointmentDate") String appointmentDate,
							@RequestParam("slotNumber") Integer slotNumber,
							RedirectAttributes redirectAttributes) {
		try {
			// Check if slot is already booked
			List<Booking> existingBookings = bookingRepository.findByDateAndSlot(appointmentDate, slotNumber);
			if (!existingBookings.isEmpty()) {
				redirectAttributes.addFlashAttribute("error", "This slot is already booked. Please choose another slot.");
				return "redirect:/dashboard";
			}

			// Get the current user using SessionManager
			String username = sessionManager.getLoggedInUser();
			if (username == null) {
				redirectAttributes.addFlashAttribute("error", "Please login first");
				return "redirect:/user/login";
			}
			
			User currentUser = userRepository.findByUsername(username);
			
			// Set the booking time (current time)
			booking.setBookingTime(LocalDateTime.now());
			
			// Set the user for the booking
			booking.setUser(currentUser);
			
			// Set the appointment date and slot information
			booking.setAppointmentDate(appointmentDate);
			booking.setSlotNumber(slotNumber);
			
			// Set slot times based on slot number
			Map<Integer, Map<String, String>> slotTimes = Map.of(
				1, Map.of("start", "08:00", "end", "09:00"),
				2, Map.of("start", "11:00", "end", "12:00"),
				3, Map.of("start", "14:00", "end", "15:00"),
				4, Map.of("start", "16:00", "end", "17:00")
			);
			
			Map<String, String> times = slotTimes.get(slotNumber);
			booking.setSlotStartTime(times.get("start"));
			booking.setSlotEndTime(times.get("end"));
			booking.setAppointmentTime(times.get("start")); // Set the appointment time to slot start time
			
			// Save the booking
			bookingRepository.save(booking);
			
			redirectAttributes.addFlashAttribute("success", 
				"Your booking has been successfully submitted!");
			
			return "redirect:/dashboard";
		} catch (Exception e) {
			e.printStackTrace();
			redirectAttributes.addFlashAttribute("error", 
				"Error: " + e.getMessage());
			
			return "redirect:/dashboard";
		}
	}

	@GetMapping("/booking-history")
	public String bookingHistory(Model model) {
		String username = sessionManager.getLoggedInUser();
		if (username == null) {
			return "redirect:/user/login";
		}
		
		User user = userRepository.findByUsername(username);
		List<Booking> bookings = bookingRepository.findByUser(user);
		model.addAttribute("bookings", bookings);
		model.addAttribute("username", username);
		return "booking-history";
	}

	@GetMapping("/modify-booking/{id}")
	public String modifyBooking(@PathVariable Long id, Model model) {
		String username = sessionManager.getLoggedInUser();
		if (username == null) {
			return "redirect:/user/login";
		}
		
		Booking booking = bookingRepository.findById(id).orElse(null);
		if (booking != null && booking.getUser().getUsername().equals(username)) {
			// Check if booking can be modified
			if (booking.getStatus().equals("CONFIRMED") || booking.getStatus().equals("COMPLETED")) {
				model.addAttribute("error", "Cannot modify a confirmed or completed booking.");
				return "redirect:/booking-history";
			}
			
			model.addAttribute("booking", booking);
			model.addAttribute("username", username);
			
			// Add slot information
			Map<Integer, Map<String, String>> slots = new HashMap<>();
			slots.put(1, Map.of("start", "08:00", "end", "09:00"));
			slots.put(2, Map.of("start", "11:00", "end", "12:00"));
			slots.put(3, Map.of("start", "14:00", "end", "15:00"));
			slots.put(4, Map.of("start", "16:00", "end", "17:00"));
			model.addAttribute("slots", slots);
			
			return "modify-booking";
		}
		return "redirect:/booking-history";
	}

	@PostMapping("/update-booking")
	public String updateBooking(@ModelAttribute Booking booking, 
							@RequestParam Long userId,
							@RequestParam("appointmentDate") String appointmentDate,
							@RequestParam("slotNumber") Integer slotNumber,
							RedirectAttributes redirectAttributes) {
		try {
			// Check if the booking exists and get its current status
			Booking existingBooking = bookingRepository.findById(booking.getId()).orElse(null);
			if (existingBooking == null) {
				redirectAttributes.addFlashAttribute("error", "Booking not found!");
				return "redirect:/booking-history";
			}

			// Check if booking can be modified
			if (existingBooking.getStatus().equals("CONFIRMED") || existingBooking.getStatus().equals("COMPLETED")) {
				redirectAttributes.addFlashAttribute("error", "Cannot modify a confirmed or completed booking.");
				return "redirect:/booking-history";
			}

			// Check if the new slot is available
			if (!existingBooking.getSlotNumber().equals(slotNumber) || 
				!existingBooking.getAppointmentDate().equals(appointmentDate)) {
				List<Booking> existingBookings = bookingRepository.findByDateAndSlot(appointmentDate, slotNumber);
				if (!existingBookings.isEmpty()) {
					redirectAttributes.addFlashAttribute("error", "This slot is already booked. Please choose another slot.");
					return "redirect:/modify-booking/" + booking.getId();
				}
			}

			// Fetch the user from the database
			User user = userRepository.findById(userId).orElse(null);
			if (user != null) {
				booking.setUser(user);
			}

			// Set slot times based on slot number
			Map<Integer, Map<String, String>> slotTimes = Map.of(
				1, Map.of("start", "08:00", "end", "09:00"),
				2, Map.of("start", "11:00", "end", "12:00"),
				3, Map.of("start", "14:00", "end", "15:00"),
				4, Map.of("start", "16:00", "end", "17:00")
			);
			
			Map<String, String> times = slotTimes.get(slotNumber);
			booking.setSlotStartTime(times.get("start"));
			booking.setSlotEndTime(times.get("end"));
			booking.setAppointmentTime(times.get("start"));
			
			// Save the booking
			bookingRepository.save(booking);
			redirectAttributes.addFlashAttribute("success", "Your booking has been successfully updated!");
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error updating booking: " + e.getMessage());
		}
		return "redirect:/booking-history";
	}

	@PostMapping("/cancel-booking/{id}")
	public String cancelBooking(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		try {
			Booking booking = bookingRepository.findById(id).orElse(null);
			if (booking != null) {
				// Check if booking can be cancelled
				if (booking.getStatus().equals("CONFIRMED") || booking.getStatus().equals("COMPLETED")) {
					redirectAttributes.addFlashAttribute("error", "Cannot cancel a confirmed or completed booking.");
					return "redirect:/booking-history";
				}
				
				bookingRepository.delete(booking);
				redirectAttributes.addFlashAttribute("success", "Your booking has been successfully canceled!");
			} else {
				redirectAttributes.addFlashAttribute("error", "Booking not found!");
			}
		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Error canceling booking: " + e.getMessage());
		}
		return "redirect:/booking-history";
	}

	@PostMapping("/user/resend-verification")
	@ResponseBody
	public Map<String, String> resendVerification(@RequestParam String email, HttpServletRequest request) {
		try {
			User user = userRepository.findByEmail(email);
			if (user != null && !user.isEnabled()) {
				// Generate new token
				String token = UUID.randomUUID().toString();
				user.setVerificationToken(token);
				user.setTokenExpiryDate(LocalDateTime.now().plusHours(24));
				userRepository.save(user);
				
				// Resend verification email
				String baseUrl = getBaseUrl(request);
				emailService.sendVerificationEmail(email, token, baseUrl);
				
				return Map.of("message", "Verification email has been resent!");
			}
			return Map.of("message", "Invalid email or account already verified");
		} catch (Exception e) {
			return Map.of("message", "Error resending verification email");
		}
	}
	
	@GetMapping("/user/forgot-password")
	public String forgotPassword() {
	    return "forgot-password";
	}

	@PostMapping("/user/forgot-password")
	public String processForgotPassword(@RequestParam("email") String email, Model model, HttpServletRequest request) {
	    User user = userRepository.findByEmail(email);
	    
	    if (user == null) {
	        model.addAttribute("error", "Email not found!");
	        return "forgot-password";
	    }
	    
	    // Generate reset token
	    String token = UUID.randomUUID().toString();
	    user.setVerificationToken(token);
	    // Set token expiry to 1 hour
	    user.setTokenExpiryDate(LocalDateTime.now().plusHours(1));
	    userRepository.save(user);
	    
	    try {
	        // Send password reset email
	        String baseUrl = getBaseUrl(request);
	        emailService.sendPasswordResetEmail(email, token, baseUrl);
	        model.addAttribute("message", "Password reset link has been sent to your email!");
	    } catch (Exception e) {
	        model.addAttribute("error", "Error sending password reset email!");
	    }
	    
	    return "forgot-password";
	}

	@GetMapping("/user/reset-password")
	public String resetPassword(@RequestParam("token") String token, Model model) {
	    User user = userRepository.findByVerificationToken(token);
	    
	    if (user == null) {
	        model.addAttribute("error", "Invalid reset token!");
	        return "Login";
	    }
	    
	    if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
	        model.addAttribute("error", "Reset token has expired!");
	        return "Login";
	    }
	    
	    model.addAttribute("token", token);
	    return "reset-password";
	}

	@PostMapping("/user/reset-password")
	public String processResetPassword(
	        @RequestParam("token") String token,
	        @RequestParam("password") String password,
	        @RequestParam("confirmPassword") String confirmPassword,
	        Model model) {
	    
	    User user = userRepository.findByVerificationToken(token);
	    
	    if (user == null) {
	        model.addAttribute("error", "Invalid reset token!");
	        return "Login";
	    }
	    
	    if (user.getTokenExpiryDate().isBefore(LocalDateTime.now())) {
	        model.addAttribute("error", "Reset token has expired!");
	        return "Login";
	    }
	    
	    if (!password.equals(confirmPassword)) {
	        model.addAttribute("error", "Passwords do not match!");
	        model.addAttribute("token", token);
	        return "reset-password";
	    }
	    
	    // Update the password with hashed version
	    user.setPassword(PasswordEncoder.encode(password));
	    user.setVerificationToken(null);
	    user.setTokenExpiryDate(null);
	    userRepository.save(user);
	    
	    model.addAttribute("message", "Password has been reset successfully! You can now login.");
	    return "Login";
	}
	
	@GetMapping("/api/available-slots")
	@ResponseBody
	public Map<String, Object> getAvailableSlots(@RequestParam String date) {
		Map<String, Object> response = new HashMap<>();
		Map<Integer, Map<String, String>> availableSlots = new HashMap<>();
		
		// Get all bookings for the selected date
		List<Booking> bookings = bookingRepository.findByDate(date);
		Set<Integer> bookedSlots = bookings.stream()
			.map(Booking::getSlotNumber)
			.collect(Collectors.toSet());
		
		// Define all possible slots
		Map<Integer, Map<String, String>> allSlots = Map.of(
			1, Map.of("start", "08:00", "end", "09:00"),
			2, Map.of("start", "11:00", "end", "12:00"),
			3, Map.of("start", "14:00", "end", "15:00"),
			4, Map.of("start", "16:00", "end", "17:00")
		);
		
		// Only include slots that are not booked
		allSlots.forEach((slotNumber, times) -> {
			if (!bookedSlots.contains(slotNumber)) {
				availableSlots.put(slotNumber, times);
			}
		});
		
		response.put("slots", availableSlots);
		return response;
	}
	
	// Utility method to build base URL
	private String getBaseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		String contextPath = request.getContextPath();
		StringBuilder url = new StringBuilder();
		url.append(scheme).append("://").append(serverName);
		if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
			url.append(":" + serverPort);
		}
		url.append(contextPath);
		return url.toString();
	}
	
}