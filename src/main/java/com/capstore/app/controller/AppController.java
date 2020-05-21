package com.capstore.app.controller;

import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import com.capstore.app.models.CustomerDetails;
import com.capstore.app.models.MerchantDetails;
import com.capstore.app.models.Product;
import com.capstore.app.models.User;
import com.capstore.app.repository.ConfirmationTokenRepository;
import com.capstore.app.repository.ProductServiceImpl;
import com.capstore.app.repository.UserRepository;
import com.capstore.app.signup_login.ConfirmationToken;
import com.capstore.app.signup_login.EmailSenderService;

import lombok.Data;

@Data
@Transactional
@RestController
@CrossOrigin(origins = "http://localhost:4200",allowedHeaders = "*")
public class AppController {
	@Autowired
	ProductServiceImpl productServiceImpl;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	ConfirmationTokenRepository confirmationTokenRepository;
	
	@Autowired
	EmailSenderService emailSenderService;

    @RequestMapping(value="Billing-App/registerCustomer", method = RequestMethod.POST)
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody CustomerDetails cd) throws MessagingException
    {
        CustomerDetails existingCustomer = userRepository.findCustomerByEmailIgnoreCase(cd.getEmail());
        if(existingCustomer != null)
        {
            return new ResponseEntity<Error>(HttpStatus.CONFLICT);
        }
        else
        {
            userRepository.saveCustomer(cd);
            CustomerDetails cd1=userRepository.findCustomerByEmailIgnoreCase(cd.getEmail());
            
            ConfirmationToken confirmationToken = new ConfirmationToken(cd1.getUserId());
            System.out.println(confirmationToken);

            confirmationTokenRepository.save(confirmationToken);

            MimeMessage mailMessage = emailSenderService.createMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true);
			
            helper.setTo(cd.getEmail());
            helper.setSubject("Complete Registration!");
            helper.setFrom("capstore06@gmail.com");
            helper.setText("To activate your account, please click here : "
                    +"http://localhost:4200/verify?token="+confirmationToken.getConfirmationToken());

            emailSenderService.sendEmail(mailMessage);
            
            return ResponseEntity.ok(HttpStatus.OK);
        }
    }

    @RequestMapping(value="Billing-App/registerMerchant", method = RequestMethod.POST)
    public ResponseEntity<?> registerMerchant(@Valid @RequestBody MerchantDetails md) throws MessagingException
    {

        MerchantDetails existingMerchant = userRepository.findMerchantByEmailIgnoreCase(md.getEmail());
        if(existingMerchant != null)
        {
            return new ResponseEntity<Error>(HttpStatus.CONFLICT);
        }
        else
        {
            userRepository.saveMerchant(md);
            
            MimeMessage mailMessage = emailSenderService.createMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true);
            String url = "http://localhost:4200/verify/"+md.getEmail();
            helper.setTo("dsonaje6@gmail.com");
            helper.setSubject("Merchantasssss Requesting Approval!");
            helper.setFrom("capstore06@gmail.com");
            helper.setText("<html><body><h1>Merchant Registration!</h1><br>" +
      			  md+"<br><button type='submit'>"
		  		+ "<a href="+url+">Show Details</a></button>",true);// + "<br><body></html>To provide Approval, please click here : "
//			  	            +"http://localhost:4200/Billing-App/confirm-account?token="+confirmationToken.getConfirmationToken(), true);
//			  
            
            emailSenderService.sendEmail(mailMessage);

            return ResponseEntity.ok(HttpStatus.OK);
        }
    }
    
    @GetMapping("/Billing-App/{email}")
	public ResponseEntity<MerchantDetails>verifyMerchantDetails(@PathVariable("email") String email){
		return new ResponseEntity<MerchantDetails>(userRepository.findMerchantByEmailIgnoreCase(email),HttpStatus.OK);
	}
    
    @PutMapping("/Billing-App/generateToken/{email}")
    public MerchantDetails generateToken(@PathVariable("email") String email) throws MessagingException{
    	MerchantDetails md1=userRepository.findMerchantByEmailIgnoreCase(email);

    	System.out.println(email);
    	
        ConfirmationToken confirmationToken = new ConfirmationToken(md1.getUserId());

        confirmationTokenRepository.save(confirmationToken);
        md1.setActive(true);
        md1.setApproved(true);
        userRepository.saveMerchant(md1);
        
        MimeMessage mailMessage = emailSenderService.createMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true);
        helper.setTo(md1.getEmail());
        helper.setSubject("Account Activated!");
        helper.setFrom("capstore06@gmail.com");
        helper.setText("Admin approved your account.\nTo login and access your account, please click here : "
        +"http://localhost:4200");

        emailSenderService.sendEmail(mailMessage);
        
        return md1;
    }
 
    @RequestMapping(value="Billing-App/confirm-account", method= {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> confirmUserAccount(@Valid  @RequestParam("token") String confirmationToken) throws MessagingException
    {
        ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);

        if(token != null)
        {
            if(userRepository.findCustomerById(token.getUid())!=null) {
            	CustomerDetails cd=userRepository.findCustomerById(token.getUid());
            	cd.setActive(true);
                userRepository.saveCustomer(cd);
            }
            else if(userRepository.findMerchantById(token.getUid())!=null) {
            	MerchantDetails md=userRepository.findMerchantById(token.getUid());
            	md.setActive(true);
                userRepository.saveMerchant(md);
                
                MimeMessage mailMessage = emailSenderService.createMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mailMessage, true);
                helper.setTo(md.getEmail());
                helper.setSubject("Account Activated!");
                helper.setFrom("capstore06@gmail.com");
                helper.setText("Admin approved your account.\nTo login and access your account, please click here : "
                +"http://localhost:4200");

                emailSenderService.sendEmail(mailMessage);
            }
            
            return ResponseEntity.ok(HttpStatus.OK);
        }
        else
        {
        	return new ResponseEntity<Error>(HttpStatus.CONFLICT);
        }
    }
    
    @RequestMapping(value="Billing-App/login", method= {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> userLogin(@Valid @RequestBody String[] userCredentials) {
    	String email=userCredentials[0];
    	String pass=userCredentials[1];
    	String role=userCredentials[2];
    	System.out.println(email+pass+role);
    	if (role.equals("Customer")) {
    		CustomerDetails cd=userRepository.findCustomerByEmailIgnoreCase(email);
    		if(cd!=null && cd.isActive()==true) {
    			if(pass.equals(cd.getPassword())) {
    				return ResponseEntity.ok().body(cd);
    			}
    		}
    	}
    	else {
    		MerchantDetails md=userRepository.findMerchantByEmailIgnoreCase(email);
    		if(md!=null && md.isActive()==true) {
    			if(pass.equals(md.getPassword())) {
    				return ResponseEntity.ok().body(md);
    			}
    		}
    	}
    	return new ResponseEntity<Error>(HttpStatus.CONFLICT);
    }
    
  //All Products Data
  	@GetMapping(value="Billing-App/allProducts")
  	public List<Product> getAllProducts(){
  		return productServiceImpl.allProductsList();
  	}
  	
  	
  	//Products data of particular category
  	@GetMapping(value="Billing-App/productCategory/{category}")
  	public List<Product> getCategory(@PathVariable("category") String productCategory){
  		
  		return productServiceImpl.specificCategoryProducts(productCategory);
  	}
  	
  	
  	//Product data based on discount
  	@GetMapping(value="Billing-App/discountCategory/{category}/{discountPercent}")
  	public List<Product> getDiscountProducts(@PathVariable("category") String productCategory,@PathVariable("discountPercent") String discount){
  		
  		return productServiceImpl.specificDiscountProducts(productCategory, discount);
  	}
  	
  	
  	@GetMapping(value="Billing-App/searchProducts/{category}")
  	public List<Product> getSearchProducts(@PathVariable("category") String productSearch){
  		return productServiceImpl.searchProducts(productSearch);
  	}
    
    // getters and setters
    
}